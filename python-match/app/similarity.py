import os
import time
from functools import lru_cache
from pathlib import Path

import torch
from sentence_transformers import SentenceTransformer, util

from app.logger import get_logger
from app.schemas import CandidateResult, EmbeddingItem, EmbeddingVector, MatchItem

logger = get_logger("similarity")

MODEL_NAME = os.getenv("MATCH_MODEL_NAME", "BM-K/KoSimCSE-roberta-multitask")
MODEL_BACKEND = os.getenv("MATCH_MODEL_BACKEND", "torch").strip().lower()
MODEL_CACHE_DIR = os.getenv("MATCH_MODEL_CACHE_DIR") or None
ONNX_SAVE_DIR = os.getenv("MATCH_ONNX_SAVE_DIR") or None
ONNX_FILE_NAME = os.getenv("MATCH_ONNX_FILE_NAME") or None
ONNX_PROVIDER = os.getenv("MATCH_ONNX_PROVIDER", "CPUExecutionProvider")
ONNX_EXPORT = os.getenv("MATCH_ONNX_EXPORT", "true").strip().lower() in {"1", "true", "yes", "on"}
SUPPORTED_BACKENDS = {"torch", "onnx"}


@lru_cache(maxsize=1)
def _load_model() -> SentenceTransformer:
    if MODEL_BACKEND not in SUPPORTED_BACKENDS:
        raise ValueError(
            f"Unsupported MATCH_MODEL_BACKEND={MODEL_BACKEND!r}. "
            f"Expected one of {sorted(SUPPORTED_BACKENDS)}."
        )

    model_name_or_path = _resolve_model_name_or_path()
    model_kwargs = _build_model_kwargs()
    logger.info(
        f"Loading model: {model_name_or_path} "
        f"(source={MODEL_NAME}, backend={MODEL_BACKEND})"
    )
    logger.info(f"모델 로딩 중: {MODEL_NAME}")
    t0 = time.perf_counter()
    model = SentenceTransformer(
        model_name_or_path,
        backend=MODEL_BACKEND,
        cache_folder=MODEL_CACHE_DIR,
        model_kwargs=model_kwargs or None,
    )
    _save_exported_onnx_model(model)
    model.encode(["워밍업"], convert_to_tensor=False)
    elapsed = time.perf_counter() - t0
    logger.info(f"모델 로딩 완료 ({elapsed:.1f}s)")
    return model


def _resolve_model_name_or_path() -> str:
    if MODEL_BACKEND != "onnx" or not ONNX_SAVE_DIR:
        return MODEL_NAME

    if _has_saved_onnx_model():
        save_dir = Path(ONNX_SAVE_DIR)
        return str(save_dir)
    return MODEL_NAME


def _build_model_kwargs() -> dict[str, object]:
    if MODEL_BACKEND != "onnx":
        return {}

    kwargs: dict[str, object] = {
        "provider": ONNX_PROVIDER,
        "export": ONNX_EXPORT and not _has_saved_onnx_model(),
    }
    if ONNX_FILE_NAME:
        kwargs["file_name"] = ONNX_FILE_NAME
    return kwargs


def _save_exported_onnx_model(model: SentenceTransformer) -> None:
    if MODEL_BACKEND != "onnx" or not ONNX_SAVE_DIR:
        return

    save_dir = Path(ONNX_SAVE_DIR)
    if (save_dir / "config_sentence_transformers.json").exists():
        return

    save_dir.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(str(save_dir))
    logger.info(f"Saved ONNX model for reuse: {save_dir}")
    _quantize_onnx_model(save_dir)


def _quantize_onnx_model(save_dir: Path) -> None:
    from onnxruntime.quantization import QuantType, quantize_dynamic

    onnx_dir = save_dir / "onnx"
    model_path = onnx_dir / "model.onnx"
    int8_path = onnx_dir / "model_int8.onnx"

    if not model_path.exists() or int8_path.exists():
        return

    logger.info("ONNX int8 동적 양자화 시작...")
    t0 = time.perf_counter()
    quantize_dynamic(str(model_path), str(int8_path), weight_type=QuantType.QInt8)
    elapsed = time.perf_counter() - t0
    logger.info(f"양자화 완료 ({elapsed:.1f}s) → {int8_path}")


def _has_saved_onnx_model() -> bool:
    if not ONNX_SAVE_DIR:
        return False

    save_dir = Path(ONNX_SAVE_DIR)
    return (save_dir / "config_sentence_transformers.json").exists()


def compute_embeddings(items: list[EmbeddingItem]) -> list[EmbeddingVector]:
    if not items:
        return []
    model = _load_model()
    texts = [item.text for item in items]
    embeddings = model.encode(texts, convert_to_numpy=True, normalize_embeddings=True)
    return [EmbeddingVector(id=item.id, vector=emb.tolist()) for item, emb in zip(items, embeddings)]


def build_match_text(item: MatchItem) -> str:
    parts: list[str] = []
    _append_text_part(parts, "물품명", item.itemName)
    _append_text_part(parts, "제목", item.title)
    _append_text_part(parts, "카테고리", item.category)
    _append_text_part(parts, "색상", item.color)
    return ". ".join(parts)


def _append_text_part(parts: list[str], label: str, value: str | None) -> None:
    if not value or not value.strip():
        return

    normalized = " ".join(value.strip().split())
    if any(existing.endswith(f": {normalized}") for existing in parts):
        return
    parts.append(f"{label}: {normalized}")


def compute_similarity(
    query: MatchItem,
    candidates: list[MatchItem],
) -> list[CandidateResult]:
    if not candidates:
        logger.debug(f"[query={query.id}] 후보 없음 → 빈 결과 반환")
        return []

    model = _load_model()

    query_text = build_match_text(query)
    candidate_texts = [build_match_text(c) for c in candidates]

    # Empty query/candidate text means "semantic unavailable" for that pair.
    # Omit those results so Java falls back to rule-only scoring.
    valid_indices = [i for i, t in enumerate(candidate_texts) if t]
    skipped = len(candidates) - len(valid_indices)

    if not query_text:
        logger.info(f"[query={query.id}] 쿼리 텍스트 없음 → 시맨틱 스킵")
        return []
    if not valid_indices:
        logger.info(f"[query={query.id}] 유효한 후보 텍스트 없음 → 시맨틱 스킵")
        return []

    logger.info(
        f"[query={query.id}] 유사도 계산 시작 | "
        f'쿼리="{query_text}" | 후보={len(valid_indices)}개'
        + (f" (텍스트 없는 후보 {skipped}개 제외)" if skipped else "")
    )
    logger.debug(
        f"[query={query.id}] 후보 목록: "
        + ", ".join(
            f"{candidates[i].id}={candidate_texts[i]!r}" for i in valid_indices
        )
    )

    texts_to_encode = [query_text] + [candidate_texts[i] for i in valid_indices]
    t0 = time.perf_counter()
    embeddings = model.encode(texts_to_encode, convert_to_tensor=True, normalize_embeddings=True)
    encode_ms = (time.perf_counter() - t0) * 1000
    logger.debug(f"[query={query.id}] 인코딩 완료 ({encode_ms:.1f}ms, {len(texts_to_encode)}개 텍스트)")

    query_emb = embeddings[0]
    candidate_embs = embeddings[1:]

    cosine_scores = util.cos_sim(query_emb, candidate_embs)[0]

    results: list[CandidateResult] = []
    for rank, i in enumerate(valid_indices):
        raw_score = cosine_scores[rank].item()
        score = round(max(0.0, min(100.0, raw_score * 100)), 2)
        logger.debug(f"[query={query.id}] id={candidates[i].id} score={score:.2f} text={candidate_texts[i]!r}")
        results.append(
            CandidateResult(
                candidateId=candidates[i].id,
                semanticScore=score,
                reasons=["물품명/제목/카테고리/색상 의미 유사"] if score >= 50.0 else [],
            )
        )

    results.sort(key=lambda r: r.candidateId)

    top = max(results, key=lambda r: r.semanticScore)
    high_count = sum(1 for r in results if r.semanticScore >= 50.0)
    logger.info(
        f"[query={query.id}] 완료 | 결과={len(results)}개 | "
        f"최고점={top.semanticScore:.2f}(id={top.candidateId}) | "
        f"50점 이상={high_count}개"
    )

    return results
