import io
import math
import os
import tempfile
import time
from functools import lru_cache

import torch
from PIL import Image
from transformers import AutoModel, AutoProcessor

from app.logger import get_logger

logger = get_logger("clip_embedding")

CLIP_MODEL_NAME = os.getenv("CLIP_MODEL_NAME", "AvitoTech/Zer0int-CLIP-L-for-animal-identification")
CLIP_PROCESSOR_NAME = os.getenv("CLIP_PROCESSOR_NAME", "zer0int/CLIP-GmP-ViT-L-14")
CLIP_TRUST_REMOTE_CODE = os.getenv("CLIP_TRUST_REMOTE_CODE", "false").strip().lower() in {"1", "true", "yes", "on"}
PET_CROP_ENABLED = os.getenv("PET_CROP_ENABLED", "true").strip().lower() in {"1", "true", "yes", "on"}
PET_CROP_YOLO_MODEL = os.getenv("PET_CROP_YOLO_MODEL", "yolo11n.pt")
PET_CROP_MIN_CONFIDENCE = float(os.getenv("PET_CROP_MIN_CONFIDENCE", "0.35"))
PET_CROP_PADDING_RATIO = float(os.getenv("PET_CROP_PADDING_RATIO", "0.18"))
ANIMAL_CROP = "ANIMAL_CROP_V2"
ORIGINAL_FALLBACK = "ORIGINAL_FALLBACK"
_TARGET_CLASS_BY_ANIMAL_TYPE = {
    "CAT": "cat",
    "DOG": "dog",
}
_SUPPORTED_DETECTION_CLASSES = {"cat", "dog"}


@lru_cache(maxsize=1)
def _load_clip_model():
    logger.info(f"CLIP 모델 로딩 중: model={CLIP_MODEL_NAME}, processor={CLIP_PROCESSOR_NAME}")
    t0 = time.perf_counter()
    processor = AutoProcessor.from_pretrained(CLIP_PROCESSOR_NAME)
    model = AutoModel.from_pretrained(CLIP_MODEL_NAME, trust_remote_code=CLIP_TRUST_REMOTE_CODE)
    model.eval()
    # warmup
    dummy = Image.new("RGB", (224, 224))
    inputs = processor(images=dummy, return_tensors="pt")
    with torch.no_grad():
        _extract_image_features(model, inputs)
    elapsed = time.perf_counter() - t0
    logger.info(f"CLIP 모델 로딩 완료 ({elapsed:.1f}s)")
    return model, processor


def encode_image(image_bytes: bytes, animal_type: str | None = None) -> tuple[list[float], str, str]:
    model, processor = _load_clip_model()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    cropped_image, crop_type = crop_pet(image, animal_type)
    inputs = processor(images=cropped_image, return_tensors="pt")
    t0 = time.perf_counter()
    with torch.no_grad():
        features = _extract_image_features(model, inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed = (time.perf_counter() - t0) * 1000
    logger.debug(f"CLIP 인코딩 완료 ({elapsed:.1f}ms, cropType={crop_type})")
    return features[0].tolist(), CLIP_MODEL_NAME, crop_type


def encode_images(image_items: list[tuple[bytes, str | None]]) -> tuple[list[list[float]], str, list[str]]:
    model, processor = _load_clip_model()
    cropped_images = []
    crop_types = []
    for image_bytes, animal_type in image_items:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        cropped_image, crop_type = crop_pet(image, animal_type)
        cropped_images.append(cropped_image)
        crop_types.append(crop_type)

    inputs = processor(images=cropped_images, return_tensors="pt")
    t0 = time.perf_counter()
    with torch.no_grad():
        features = _extract_image_features(model, inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed = (time.perf_counter() - t0) * 1000
    logger.debug(f"CLIP batch encoding complete ({elapsed:.1f}ms, count={len(image_items)})")
    return features.tolist(), CLIP_MODEL_NAME, crop_types


def crop_pet(image: Image.Image, animal_type: str | None = None) -> tuple[Image.Image, str]:
    if not PET_CROP_ENABLED:
        return image, ORIGINAL_FALLBACK

    try:
        detector = _load_pet_detector()
        results = detector.predict(source=image, verbose=False)
        bbox = _select_pet_bbox(results, image.size, animal_type)
        if bbox is None:
            return image, ORIGINAL_FALLBACK
        return image.crop(_expand_to_square(bbox, image.size)), ANIMAL_CROP
    except Exception:
        logger.exception("반려동물 crop 실패, 원본 이미지로 fallback")
        return image, ORIGINAL_FALLBACK


def _extract_image_features(model, inputs):
    if hasattr(model, "get_image_features"):
        return model.get_image_features(**inputs)

    outputs = model(**inputs)
    if hasattr(outputs, "image_embeds"):
        return outputs.image_embeds
    if hasattr(outputs, "pooler_output"):
        return outputs.pooler_output
    raise RuntimeError("CLIP image feature output을 찾을 수 없습니다.")


@lru_cache(maxsize=1)
def _load_pet_detector():
    os.environ.setdefault("YOLO_CONFIG_DIR", os.path.join(tempfile.gettempdir(), "Ultralytics"))
    from ultralytics import YOLO

    logger.info(f"반려동물 crop detector 로딩 중: {PET_CROP_YOLO_MODEL}")
    return YOLO(PET_CROP_YOLO_MODEL)


def _select_pet_bbox(results, image_size: tuple[int, int], animal_type: str | None):
    if not results:
        return None

    target_class = _TARGET_CLASS_BY_ANIMAL_TYPE.get((animal_type or "").strip().upper())
    width, height = image_size
    image_area = max(1, width * height)
    image_cx = width / 2
    image_cy = height / 2
    max_center_distance = ((width / 2) ** 2 + (height / 2) ** 2) ** 0.5

    best = None
    best_score = -1.0
    for result in results:
        names = result.names or {}
        boxes = getattr(result, "boxes", None)
        if boxes is None:
            continue

        xyxy_list = boxes.xyxy.cpu().tolist()
        conf_list = boxes.conf.cpu().tolist()
        cls_list = boxes.cls.cpu().tolist()
        for xyxy, confidence, class_id in zip(xyxy_list, conf_list, cls_list):
            class_name = names.get(int(class_id), str(int(class_id)))
            if class_name not in _SUPPORTED_DETECTION_CLASSES:
                continue
            if confidence < PET_CROP_MIN_CONFIDENCE:
                continue
            if target_class and class_name != target_class:
                continue

            x1, y1, x2, y2 = xyxy
            box_width = max(0.0, x2 - x1)
            box_height = max(0.0, y2 - y1)
            if box_width <= 0 or box_height <= 0:
                continue

            box_cx = x1 + box_width / 2
            box_cy = y1 + box_height / 2
            distance = ((box_cx - image_cx) ** 2 + (box_cy - image_cy) ** 2) ** 0.5
            center_score = 1.0 - min(1.0, distance / max_center_distance)
            area_score = min(1.0, (box_width * box_height) / image_area)
            score = confidence * 2.0 + center_score + area_score
            if score > best_score:
                best = (x1, y1, x2, y2)
                best_score = score

    return best


def _expand_to_square(bbox, image_size: tuple[int, int]) -> tuple[int, int, int, int]:
    width, height = image_size
    x1, y1, x2, y2 = bbox
    box_width = max(0.0, x2 - x1)
    box_height = max(0.0, y2 - y1)
    side = math.ceil(max(box_width, box_height) * (1.0 + PET_CROP_PADDING_RATIO * 2.0))
    side = max(1, min(side, width, height))
    cx = (x1 + x2) / 2
    cy = (y1 + y2) / 2

    left = int(round(cx - side / 2))
    top = int(round(cy - side / 2))
    left = max(0, min(left, width - side))
    top = max(0, min(top, height - side))

    return left, top, left + side, top + side
