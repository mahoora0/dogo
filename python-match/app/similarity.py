import os
import time
from functools import lru_cache

import torch
from sentence_transformers import SentenceTransformer, util

from app.logger import get_logger
from app.schemas import CandidateResult, MatchItem

logger = get_logger("similarity")

MODEL_NAME = os.getenv("MATCH_MODEL_NAME", "BM-K/KoSimCSE-roberta-multitask")


@lru_cache(maxsize=1)
def _load_model() -> SentenceTransformer:
    logger.info(f"모델 로딩 중: {MODEL_NAME}")
    t0 = time.perf_counter()
    model = SentenceTransformer(MODEL_NAME)
    elapsed = time.perf_counter() - t0
    logger.info(f"모델 로딩 완료 ({elapsed:.1f}s)")
    return model


def build_match_text(item: MatchItem) -> str:
    if item.itemName and item.itemName.strip():
        return item.itemName.strip()
    if item.title and item.title.strip():
        return item.title.strip()
    return ""


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
                reasons=["물품명/제목 의미 유사"] if score >= 50.0 else [],
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
