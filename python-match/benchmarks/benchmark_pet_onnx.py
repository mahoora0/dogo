import argparse
import json
import os
import sys
import time
from pathlib import Path

import numpy as np
import onnxruntime as ort
import torch
from onnxruntime.quantization import QuantType, quantize_dynamic
from PIL import Image

os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.clip_embedding import (
    CLIP_MODEL_NAME,
    CLIP_PROCESSOR_NAME,
    _extract_image_features,
    _load_clip_model,
    crop_pets,
)


class ImageFeatureWrapper(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, pixel_values):
        if hasattr(self.model, "get_image_features"):
            return self.model.get_image_features(pixel_values=pixel_values)

        outputs = self.model(pixel_values=pixel_values)
        if hasattr(outputs, "image_embeds"):
            return outputs.image_embeds
        if hasattr(outputs, "pooler_output"):
            return outputs.pooler_output
        raise RuntimeError("Could not find image feature output.")


def _normalize(vectors: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(vectors, axis=-1, keepdims=True)
    return vectors / np.maximum(norms, 1e-12)


def _cosine_rows(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    return np.sum(a * b, axis=-1)


def _image_paths(image_dir: Path, limit: int) -> list[Path]:
    patterns = ("*.jpg", "*.jpeg", "*.png", "*.webp")
    paths: list[Path] = []
    for pattern in patterns:
        paths.extend(image_dir.glob(pattern))
    return sorted(paths)[:limit]


def export_onnx(fp32_path: Path) -> None:
    if fp32_path.exists():
        return

    fp32_path.parent.mkdir(parents=True, exist_ok=True)
    model, processor = _load_clip_model()
    wrapper = ImageFeatureWrapper(model).eval()
    dummy = Image.new("RGB", (224, 224))
    inputs = processor(images=[dummy], return_tensors="pt")
    pixel_values = inputs["pixel_values"]

    with torch.inference_mode():
        torch.onnx.export(
            wrapper,
            (pixel_values,),
            str(fp32_path),
            input_names=["pixel_values"],
            output_names=["image_features"],
            dynamic_axes={
                "pixel_values": {0: "batch"},
                "image_features": {0: "batch"},
            },
            opset_version=17,
            do_constant_folding=True,
            dynamo=False,
        )


def quantize_onnx(fp32_path: Path, int8_path: Path) -> None:
    if int8_path.exists():
        return

    int8_path.parent.mkdir(parents=True, exist_ok=True)
    quantize_dynamic(
        model_input=str(fp32_path),
        model_output=str(int8_path),
        weight_type=QuantType.QInt8,
        per_channel=False,
    )


def run_torch(pixel_values: torch.Tensor) -> tuple[np.ndarray, float]:
    model, _ = _load_clip_model()
    t0 = time.perf_counter()
    with torch.inference_mode():
        features = _extract_image_features(model, {"pixel_values": pixel_values})
        features = features / features.norm(dim=-1, keepdim=True)
    elapsed_ms = (time.perf_counter() - t0) * 1000
    return features.cpu().numpy(), elapsed_ms


def run_ort(model_path: Path, pixel_values: torch.Tensor) -> tuple[np.ndarray, float]:
    session = ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])
    ort_inputs = {"pixel_values": pixel_values.cpu().numpy()}
    t0 = time.perf_counter()
    outputs = session.run(None, ort_inputs)[0]
    elapsed_ms = (time.perf_counter() - t0) * 1000
    return _normalize(outputs), elapsed_ms


def benchmark(image_dir: Path, output_dir: Path, limit: int) -> dict:
    fp32_path = output_dir / "pet-image-fp32.onnx"
    int8_path = output_dir / "pet-image-qint8.onnx"

    export_onnx(fp32_path)
    quantize_onnx(fp32_path, int8_path)

    paths = _image_paths(image_dir, limit)
    if not paths:
        raise RuntimeError(f"No images found in {image_dir}")

    _, processor = _load_clip_model()
    images = [Image.open(path).convert("RGB") for path in paths]
    cropped_images, crop_types = crop_pets(images, [None] * len(images))
    inputs = processor(images=cropped_images, return_tensors="pt")
    pixel_values = inputs["pixel_values"]

    torch_vectors, torch_ms = run_torch(pixel_values)
    fp32_vectors, fp32_ms = run_ort(fp32_path, pixel_values)
    int8_vectors, int8_ms = run_ort(int8_path, pixel_values)

    fp32_cos = _cosine_rows(torch_vectors, fp32_vectors)
    int8_cos = _cosine_rows(torch_vectors, int8_vectors)
    torch_pairwise = torch_vectors @ torch_vectors.T
    int8_pairwise = int8_vectors @ int8_vectors.T
    off_diagonal = ~np.eye(len(paths), dtype=bool)
    pairwise_delta = np.abs(torch_pairwise - int8_pairwise)[off_diagonal]
    top1_same = 0
    for index in range(len(paths)):
        torch_order = np.argsort(-torch_pairwise[index])
        int8_order = np.argsort(-int8_pairwise[index])
        torch_top1 = int(torch_order[torch_order != index][0])
        int8_top1 = int(int8_order[int8_order != index][0])
        if torch_top1 == int8_top1:
            top1_same += 1

    result = {
        "model": CLIP_MODEL_NAME,
        "processor": CLIP_PROCESSOR_NAME,
        "imageCount": len(paths),
        "paths": [str(path) for path in paths],
        "cropTypes": crop_types,
        "timingMs": {
            "torch": round(torch_ms, 2),
            "onnxFp32": round(fp32_ms, 2),
            "onnxInt8": round(int8_ms, 2),
        },
        "speedup": {
            "onnxFp32VsTorch": round(torch_ms / fp32_ms, 3) if fp32_ms else None,
            "onnxInt8VsTorch": round(torch_ms / int8_ms, 3) if int8_ms else None,
        },
        "cosineVsTorch": {
            "onnxFp32Mean": round(float(fp32_cos.mean()), 6),
            "onnxFp32Min": round(float(fp32_cos.min()), 6),
            "onnxInt8Mean": round(float(int8_cos.mean()), 6),
            "onnxInt8Min": round(float(int8_cos.min()), 6),
        },
        "pairwiseSearchImpact": {
            "int8AbsDeltaMean": round(float(pairwise_delta.mean()), 6),
            "int8AbsDeltaMax": round(float(pairwise_delta.max()), 6),
            "int8Top1Consistency": f"{top1_same}/{len(paths)}",
        },
        "onnxFiles": {
            "fp32": str(fp32_path),
            "int8": str(int8_path),
            "fp32Bytes": fp32_path.stat().st_size,
            "int8Bytes": int8_path.stat().st_size,
        },
    }
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image-dir", type=Path, default=Path.home() / "dogo" / "uploads" / "animal-reports")
    parser.add_argument("--output-dir", type=Path, default=Path(".onnx") / "pet-image")
    parser.add_argument("--limit", type=int, default=11)
    parser.add_argument("--result-json", type=Path, default=Path("benchmarks") / "pet_onnx_result.json")
    args = parser.parse_args()

    result = benchmark(args.image_dir, args.output_dir, args.limit)
    args.result_json.parent.mkdir(parents=True, exist_ok=True)
    args.result_json.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
