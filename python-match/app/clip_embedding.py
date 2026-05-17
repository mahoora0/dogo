import io
import os
import time
from functools import lru_cache

import torch
from PIL import Image
from transformers import CLIPModel, CLIPProcessor

from app.logger import get_logger

logger = get_logger("clip_embedding")

CLIP_MODEL_NAME = os.getenv("CLIP_MODEL_NAME", "openai/clip-vit-base-patch32")


@lru_cache(maxsize=1)
def _load_clip_model() -> tuple[CLIPModel, CLIPProcessor]:
    logger.info(f"CLIP 모델 로딩 중: {CLIP_MODEL_NAME}")
    t0 = time.perf_counter()
    processor = CLIPProcessor.from_pretrained(CLIP_MODEL_NAME)
    model = CLIPModel.from_pretrained(CLIP_MODEL_NAME)
    model.eval()
    # warmup
    dummy = Image.new("RGB", (224, 224))
    inputs = processor(images=dummy, return_tensors="pt")
    with torch.no_grad():
        model.get_image_features(**inputs)
    elapsed = time.perf_counter() - t0
    logger.info(f"CLIP 모델 로딩 완료 ({elapsed:.1f}s)")
    return model, processor


def encode_image(image_bytes: bytes) -> list[float]:
    model, processor = _load_clip_model()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    inputs = processor(images=image, return_tensors="pt")
    t0 = time.perf_counter()
    with torch.no_grad():
        features = model.get_image_features(**inputs)
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed = (time.perf_counter() - t0) * 1000
    logger.debug(f"CLIP 인코딩 완료 ({elapsed:.1f}ms)")
    return features[0].tolist()
