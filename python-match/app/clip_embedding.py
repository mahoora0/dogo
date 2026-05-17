import io
import os
import time
from functools import lru_cache

import torch
from PIL import Image
from transformers import AutoModel, AutoProcessor

from app.logger import get_logger

logger = get_logger("clip_embedding")

CLIP_MODEL_NAME = os.getenv("CLIP_MODEL_NAME", "AvitoTech/CLIP-ViT-base-for-animal-identification")
CLIP_PROCESSOR_NAME = os.getenv("CLIP_PROCESSOR_NAME", "openai/clip-vit-base-patch32")
CLIP_TRUST_REMOTE_CODE = os.getenv("CLIP_TRUST_REMOTE_CODE", "false").strip().lower() in {"1", "true", "yes", "on"}
DEFAULT_CROP_TYPE = "ORIGINAL_FALLBACK"


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


def encode_image(image_bytes: bytes) -> tuple[list[float], str, str]:
    model, processor = _load_clip_model()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    inputs = processor(images=image, return_tensors="pt")
    t0 = time.perf_counter()
    with torch.no_grad():
        features = _extract_image_features(model, inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed = (time.perf_counter() - t0) * 1000
    logger.debug(f"CLIP 인코딩 완료 ({elapsed:.1f}ms)")
    return features[0].tolist(), CLIP_MODEL_NAME, DEFAULT_CROP_TYPE


def _extract_image_features(model, inputs):
    if hasattr(model, "get_image_features"):
        return model.get_image_features(**inputs)

    outputs = model(**inputs)
    if hasattr(outputs, "image_embeds"):
        return outputs.image_embeds
    if hasattr(outputs, "pooler_output"):
        return outputs.pooler_output
    raise RuntimeError("CLIP image feature output을 찾을 수 없습니다.")
