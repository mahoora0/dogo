import io
import math
import os
import tempfile
import time
from functools import lru_cache
from pathlib import Path

import torch
from PIL import Image
from transformers import AutoModel, AutoProcessor

from app.logger import get_logger

logger = get_logger("clip_embedding")

CLIP_MODEL_NAME = os.getenv("CLIP_MODEL_NAME", "AvitoTech/Zer0int-CLIP-L-for-animal-identification")
CLIP_PROCESSOR_NAME = os.getenv("CLIP_PROCESSOR_NAME", "zer0int/CLIP-GmP-ViT-L-14")
CLIP_MODEL_BACKEND = os.getenv("CLIP_MODEL_BACKEND", "torch").strip().lower()
CLIP_ONNX_MODEL_PATH = os.getenv("CLIP_ONNX_MODEL_PATH", ".onnx/pet-image/pet-image-qint8.onnx")
CLIP_EMBEDDING_MODEL_NAME = os.getenv(
    "CLIP_EMBEDDING_MODEL_NAME",
    f"{CLIP_MODEL_NAME}:{CLIP_MODEL_BACKEND}" if CLIP_MODEL_BACKEND != "torch" else CLIP_MODEL_NAME,
)
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
    if CLIP_MODEL_BACKEND in {"onnx", "onnx-int8"}:
        import onnxruntime as ort

        onnx_path = Path(CLIP_ONNX_MODEL_PATH)
        if not onnx_path.exists():
            raise FileNotFoundError(f"CLIP ONNX model not found: {onnx_path}")
        model = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    elif CLIP_MODEL_BACKEND == "torch":
        model = AutoModel.from_pretrained(CLIP_MODEL_NAME, trust_remote_code=CLIP_TRUST_REMOTE_CODE)
        model.eval()
    else:
        raise ValueError(f"Unsupported CLIP_MODEL_BACKEND: {CLIP_MODEL_BACKEND}")
    # warmup
    dummy = Image.new("RGB", (224, 224))
    inputs = processor(images=dummy, return_tensors="pt")
    with torch.inference_mode():
        _extract_image_features(model, inputs)
    elapsed = time.perf_counter() - t0
    logger.info(f"CLIP 모델 로딩 완료 ({elapsed:.1f}s)")
    logger.info(f"CLIP backend ready: backend={CLIP_MODEL_BACKEND}, embeddingModel={CLIP_EMBEDDING_MODEL_NAME}")
    return model, processor


def encode_image(image_bytes: bytes, animal_type: str | None = None) -> tuple[list[float], str, str]:
    model, processor = _load_clip_model()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    cropped_image, crop_type = crop_pet(image, animal_type)
    inputs = processor(images=cropped_image, return_tensors="pt")
    t0 = time.perf_counter()
    with torch.inference_mode():
        features = _extract_image_features(model, inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed = (time.perf_counter() - t0) * 1000
    logger.debug(f"CLIP 인코딩 완료 ({elapsed:.1f}ms, cropType={crop_type})")
    return features[0].tolist(), CLIP_EMBEDDING_MODEL_NAME, crop_type


def encode_images(image_items: list[tuple[bytes, str | None]]) -> tuple[list[list[float]], str, list[str]]:
    model, processor = _load_clip_model()
    t0 = time.perf_counter()
    images = [Image.open(io.BytesIO(image_bytes)).convert("RGB") for image_bytes, _ in image_items]
    animal_types = [animal_type for _, animal_type in image_items]
    t_decode = time.perf_counter()
    cropped_images, crop_types = crop_pets(images, animal_types)
    t_crop = time.perf_counter()
    inputs = processor(images=cropped_images, return_tensors="pt")
    t_preprocess = time.perf_counter()
    with torch.inference_mode():
        features = _extract_image_features(model, inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    t_encode = time.perf_counter()
    logger.info(
        "CLIP batch complete: count=%d, decode=%.1fms, crop=%.1fms, preprocess=%.1fms, encode=%.1fms, total=%.1fms",
        len(image_items),
        (t_decode - t0) * 1000,
        (t_crop - t_decode) * 1000,
        (t_preprocess - t_crop) * 1000,
        (t_encode - t_preprocess) * 1000,
        (t_encode - t0) * 1000,
    )
    return features.tolist(), CLIP_EMBEDDING_MODEL_NAME, crop_types


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


def crop_pets(images: list[Image.Image], animal_types: list[str | None]) -> tuple[list[Image.Image], list[str]]:
    if not PET_CROP_ENABLED:
        return images, [ORIGINAL_FALLBACK] * len(images)

    try:
        detector = _load_pet_detector()
        results = detector.predict(source=images, verbose=False)
        cropped_images = []
        crop_types = []
        for image, result, animal_type in zip(images, results, animal_types):
            bbox = _select_pet_bbox([result], image.size, animal_type)
            if bbox is None:
                cropped_images.append(image)
                crop_types.append(ORIGINAL_FALLBACK)
                continue
            cropped_images.append(image.crop(_expand_to_square(bbox, image.size)))
            crop_types.append(ANIMAL_CROP)
        return cropped_images, crop_types
    except Exception:
        logger.exception("Batch pet crop failed, falling back to original images")
        return images, [ORIGINAL_FALLBACK] * len(images)


def _extract_image_features(model, inputs):
    if hasattr(model, "run") and hasattr(model, "get_inputs"):
        pixel_values = inputs["pixel_values"].detach().cpu().numpy()
        return torch.from_numpy(model.run(None, {"pixel_values": pixel_values})[0])

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
