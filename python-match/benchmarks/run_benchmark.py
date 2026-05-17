from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from statistics import mean
from typing import Any

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
if str(PACKAGE_ROOT) not in sys.path:
    sys.path.insert(0, str(PACKAGE_ROOT))

from app.schemas import MatchItem
from app.similarity import MODEL_NAME, compute_similarity


DEFAULT_DATASET = Path(__file__).with_name("match_cases.json")


@dataclass(frozen=True)
class RankedResult:
    candidate_id: int
    score: float
    rank: int


@dataclass(frozen=True)
class CaseReport:
    case_id: str
    expected_ids: set[int]
    hard_negative_ids: set[int]
    ranked: list[RankedResult]
    best_positive_score: float | None
    best_hard_negative_score: float | None

    @property
    def top_candidate_id(self) -> int | None:
        return self.ranked[0].candidate_id if self.ranked else None

    @property
    def positive_ranks(self) -> list[int]:
        return [r.rank for r in self.ranked if r.candidate_id in self.expected_ids]

    @property
    def first_positive_rank(self) -> int | None:
        return min(self.positive_ranks, default=None)

    @property
    def best_gap(self) -> float | None:
        if self.best_positive_score is None or self.best_hard_negative_score is None:
            return None
        return self.best_positive_score - self.best_hard_negative_score


def main() -> int:
    args = parse_args()
    payload = load_dataset(args.dataset)
    reports = [evaluate_case(case) for case in payload["cases"]]
    summary = summarize(reports, args.top_k, args.threshold)

    if args.json:
        print(json.dumps(summary, ensure_ascii=False, indent=2))
    else:
        print_human_report(summary, reports, args.top_k, args.threshold)

    if args.fail_under_top1 is not None and summary["top1Accuracy"] < args.fail_under_top1:
        return 1
    if args.fail_under_topk is not None and summary["topKRecall"] < args.fail_under_topk:
        return 1
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the labeled semantic-matching benchmark."
    )
    parser.add_argument(
        "--dataset",
        type=Path,
        default=DEFAULT_DATASET,
        help=f"Benchmark JSON file. Default: {DEFAULT_DATASET}",
    )
    parser.add_argument(
        "--top-k",
        type=int,
        default=3,
        help="K for recall@K. Default: 3",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=50.0,
        help="Score threshold for candidate-level precision/recall. Default: 50.0",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Print machine-readable JSON instead of the text report.",
    )
    parser.add_argument(
        "--fail-under-top1",
        type=float,
        default=None,
        help="Exit with code 1 if top-1 accuracy is below this ratio, e.g. 0.75.",
    )
    parser.add_argument(
        "--fail-under-topk",
        type=float,
        default=None,
        help="Exit with code 1 if recall@K is below this ratio, e.g. 0.90.",
    )
    return parser.parse_args()


def load_dataset(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as f:
        payload = json.load(f)
    cases = payload.get("cases")
    if not isinstance(cases, list) or not cases:
        raise ValueError(f"dataset has no cases: {path}")
    return payload


def evaluate_case(case: dict[str, Any]) -> CaseReport:
    case_id = str(case["id"])
    expected_ids = {int(i) for i in case["expectedIds"]}
    hard_negative_ids = {int(i) for i in case.get("hardNegativeIds", [])}
    query = MatchItem.model_validate(case["query"])
    candidates = [MatchItem.model_validate(candidate) for candidate in case["candidates"]]

    raw_results = compute_similarity(query, candidates)
    ranked = [
        RankedResult(candidate_id=int(result.candidateId), score=float(result.semanticScore), rank=rank)
        for rank, result in enumerate(
            sorted(raw_results, key=lambda r: (-r.semanticScore, r.candidateId)),
            start=1,
        )
    ]
    scores = {result.candidate_id: result.score for result in ranked}
    positive_scores = [scores[i] for i in expected_ids if i in scores]
    hard_negative_scores = [scores[i] for i in hard_negative_ids if i in scores]

    return CaseReport(
        case_id=case_id,
        expected_ids=expected_ids,
        hard_negative_ids=hard_negative_ids,
        ranked=ranked,
        best_positive_score=max(positive_scores, default=None),
        best_hard_negative_score=max(hard_negative_scores, default=None),
    )


def summarize(reports: list[CaseReport], top_k: int, threshold: float) -> dict[str, Any]:
    case_count = len(reports)
    first_ranks = [report.first_positive_rank for report in reports]
    top1_hits = sum(1 for rank in first_ranks if rank == 1)
    topk_hits = sum(1 for rank in first_ranks if rank is not None and rank <= top_k)
    reciprocal_ranks = [0.0 if rank is None else 1.0 / rank for rank in first_ranks]

    positive_scores = [
        report.best_positive_score for report in reports if report.best_positive_score is not None
    ]
    gaps = [report.best_gap for report in reports if report.best_gap is not None]
    threshold_metrics = summarize_threshold(reports, threshold)

    return {
        "model": MODEL_NAME,
        "caseCount": case_count,
        "top1Accuracy": round(top1_hits / case_count, 4),
        "topK": top_k,
        "topKRecall": round(topk_hits / case_count, 4),
        "mrr": round(mean(reciprocal_ranks), 4),
        "meanBestPositiveScore": round(mean(positive_scores), 2) if positive_scores else None,
        "meanPositiveHardNegativeGap": round(mean(gaps), 2) if gaps else None,
        "threshold": threshold,
        "thresholdMetrics": threshold_metrics,
        "failures": [
            {
                "caseId": report.case_id,
                "expectedIds": sorted(report.expected_ids),
                "topCandidateId": report.top_candidate_id,
                "firstPositiveRank": report.first_positive_rank,
                "topResults": [
                    {"candidateId": result.candidate_id, "score": result.score}
                    for result in report.ranked[:top_k]
                ],
            }
            for report in reports
            if report.first_positive_rank != 1
        ],
    }


def summarize_threshold(reports: list[CaseReport], threshold: float) -> dict[str, Any]:
    tp = fp = fn = 0
    for report in reports:
        scored_ids = {result.candidate_id for result in report.ranked}
        predicted_ids = {
            result.candidate_id for result in report.ranked if result.score >= threshold
        }
        tp += len(predicted_ids & report.expected_ids)
        fp += len(predicted_ids - report.expected_ids)
        fn += len((report.expected_ids & scored_ids) - predicted_ids)

    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {
        "truePositive": tp,
        "falsePositive": fp,
        "falseNegative": fn,
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
    }


def print_human_report(
    summary: dict[str, Any],
    reports: list[CaseReport],
    top_k: int,
    threshold: float,
) -> None:
    print(f"Model: {summary['model']}")
    print(f"Cases: {summary['caseCount']}")
    print(f"Top-1 accuracy: {summary['top1Accuracy']:.2%}")
    print(f"Recall@{top_k}: {summary['topKRecall']:.2%}")
    print(f"MRR: {summary['mrr']:.4f}")
    print(f"Mean best positive score: {summary['meanBestPositiveScore']}")
    print(f"Mean positive-hard-negative gap: {summary['meanPositiveHardNegativeGap']}")
    threshold_metrics = summary["thresholdMetrics"]
    print(
        f"Threshold {threshold:.1f}: "
        f"precision={threshold_metrics['precision']:.2%}, "
        f"recall={threshold_metrics['recall']:.2%}, "
        f"f1={threshold_metrics['f1']:.2%}"
    )
    print()
    print("Per-case top results:")
    for report in reports:
        status = "PASS" if report.first_positive_rank == 1 else "FAIL"
        top_results = ", ".join(
            f"{result.candidate_id}:{result.score:.2f}" for result in report.ranked[:top_k]
        )
        print(
            f"- {status} {report.case_id}: "
            f"expected={sorted(report.expected_ids)}, "
            f"firstPositiveRank={report.first_positive_rank}, "
            f"top{top_k}=[{top_results}], "
            f"gap={format_optional(report.best_gap)}"
        )


def format_optional(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.2f}"


if __name__ == "__main__":
    raise SystemExit(main())
