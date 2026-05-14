import os
from functools import lru_cache

import torch
from sentence_transformers import SentenceTransformer, util

from app.schemas import MatchItem, CandidateResult

MODEL_NAME = os.getenv("MATCH_MODEL_NAME", "BM-K/KoSimCSE-roberta-multitask")


@lru_cache(maxsize=1)
def _load_model() -> SentenceTransformer:
    return SentenceTransformer(MODEL_NAME)


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
        return []

    model = _load_model()

    query_text = build_match_text(query)
    candidate_texts = [build_match_text(c) for c in candidates]

    results: list[CandidateResult] = []

    # Candidates with empty text get score 0.0 immediately.
    valid_indices = [i for i, t in enumerate(candidate_texts) if t]
    empty_indices = [i for i, t in enumerate(candidate_texts) if not t]

    for i in empty_indices:
        results.append(
            CandidateResult(
                candidateId=candidates[i].id,
                semanticScore=0.0,
                reasons=[],
            )
        )

    if not query_text or not valid_indices:
        for i in valid_indices:
            results.append(
                CandidateResult(
                    candidateId=candidates[i].id,
                    semanticScore=0.0,
                    reasons=[],
                )
            )
        results.sort(key=lambda r: r.candidateId)
        return results

    texts_to_encode = [query_text] + [candidate_texts[i] for i in valid_indices]
    embeddings = model.encode(texts_to_encode, convert_to_tensor=True, normalize_embeddings=True)

    query_emb = embeddings[0]
    candidate_embs = embeddings[1:]

    cosine_scores = util.cos_sim(query_emb, candidate_embs)[0]

    for rank, i in enumerate(valid_indices):
        raw_score = cosine_scores[rank].item()
        score = round(max(0.0, min(100.0, raw_score * 100)), 2)
        results.append(
            CandidateResult(
                candidateId=candidates[i].id,
                semanticScore=score,
                reasons=["물품명/제목 의미 유사"] if score >= 50.0 else [],
            )
        )

    results.sort(key=lambda r: r.candidateId)
    return results
