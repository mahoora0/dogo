#!/usr/bin/env python3
"""분실물/습득물 매칭 벤치마크 러너.

사용법:
  python benchmark/run.py                  # 기본 실행 (threshold=50)
  python benchmark/run.py -v               # 케이스별 상세 출력
  python benchmark/run.py --find-best      # 최적 임계값 탐색
  python benchmark/run.py --save out.json  # 결과 JSON 저장
"""

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.schemas import MatchItem
from app.similarity import compute_similarity

DATASET_PATH = Path(__file__).parent / "dataset.json"
DEFAULT_THRESHOLD = 50.0


@dataclass
class CaseResult:
    id: str
    description: str
    difficulty: str
    expected: bool
    score: float
    predicted: bool

    @property
    def correct(self) -> bool:
        return self.predicted == self.expected


def run_benchmark(threshold: float = DEFAULT_THRESHOLD) -> tuple[list[CaseResult], dict]:
    with open(DATASET_PATH, encoding="utf-8") as f:
        dataset = json.load(f)

    results = []
    for case in dataset:
        query = MatchItem(**case["query"])
        candidate = MatchItem(**case["candidate"])

        sim_results = compute_similarity(query, [candidate])
        score = sim_results[0].semanticScore if sim_results else 0.0

        results.append(
            CaseResult(
                id=case["id"],
                description=case.get("description", ""),
                difficulty=case.get("difficulty", "unknown"),
                expected=case["expected"],
                score=score,
                predicted=score >= threshold,
            )
        )

    return results, _compute_metrics(results, threshold)


def _compute_metrics(results: list[CaseResult], threshold: float) -> dict:
    tp = sum(1 for r in results if r.expected and r.predicted)
    fp = sum(1 for r in results if not r.expected and r.predicted)
    tn = sum(1 for r in results if not r.expected and not r.predicted)
    fn = sum(1 for r in results if r.expected and not r.predicted)

    accuracy = (tp + tn) / len(results) if results else 0
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

    pos_scores = [r.score for r in results if r.expected]
    neg_scores = [r.score for r in results if not r.expected]

    return {
        "threshold": threshold,
        "total": len(results),
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "tp": tp,
        "fp": fp,
        "tn": tn,
        "fn": fn,
        "pos_avg": sum(pos_scores) / len(pos_scores) if pos_scores else 0,
        "pos_min": min(pos_scores) if pos_scores else 0,
        "pos_max": max(pos_scores) if pos_scores else 0,
        "neg_avg": sum(neg_scores) / len(neg_scores) if neg_scores else 0,
        "neg_min": min(neg_scores) if neg_scores else 0,
        "neg_max": max(neg_scores) if neg_scores else 0,
        "separation": (sum(pos_scores) / len(pos_scores) if pos_scores else 0)
        - (sum(neg_scores) / len(neg_scores) if neg_scores else 0),
    }


def find_best_threshold(step: float = 1.0) -> dict:
    thresholds = [t / 10 for t in range(200, 900, int(step * 10))]
    best: dict = {"f1": -1}
    for t in thresholds:
        _, metrics = run_benchmark(threshold=t)
        if metrics["f1"] > best["f1"]:
            best = metrics
    return best


def _metrics_by_difficulty(results: list[CaseResult], threshold: float) -> dict:
    difficulties = sorted({r.difficulty for r in results})
    out = {}
    for d in difficulties:
        subset = [r for r in results if r.difficulty == d]
        m = _compute_metrics(subset, threshold)
        out[d] = {
            "total": m["total"],
            "accuracy": m["accuracy"],
            "f1": m["f1"],
        }
    return out


def print_report(results: list[CaseResult], metrics: dict, verbose: bool = False) -> None:
    W = 62
    print(f"\n{'='*W}")
    print(f"  벤치마크 결과  (threshold={metrics['threshold']:.1f})")
    print(f"{'='*W}")
    print(f"  총 케이스  : {metrics['total']}  (매칭 기대 {sum(r.expected for r in results)}개 / 비매칭 기대 {sum(not r.expected for r in results)}개)")
    print(f"  정확도     : {metrics['accuracy']:.1%}")
    print(f"  정밀도     : {metrics['precision']:.1%}")
    print(f"  재현율     : {metrics['recall']:.1%}")
    print(f"  F1         : {metrics['f1']:.1%}")
    print(f"  혼동행렬   : TP={metrics['tp']} FP={metrics['fp']} TN={metrics['tn']} FN={metrics['fn']}")

    print(f"\n  점수 분포 ─────────────────────────────────────────")
    print(f"  매칭  케이스 점수  avg={metrics['pos_avg']:.1f}  min={metrics['pos_min']:.1f}  max={metrics['pos_max']:.1f}")
    print(f"  비매칭 케이스 점수 avg={metrics['neg_avg']:.1f}  min={metrics['neg_min']:.1f}  max={metrics['neg_max']:.1f}")
    print(f"  분리도 (avg gap)   {metrics['separation']:+.1f}")

    by_diff = _metrics_by_difficulty(results, metrics["threshold"])
    print(f"\n  난이도별 정확도 ────────────────────────────────────")
    for d, m in by_diff.items():
        correct = sum(1 for r in results if r.difficulty == d and r.correct)
        print(f"  {d:<8}  {correct}/{m['total']}  accuracy={m['accuracy']:.1%}  F1={m['f1']:.1%}")

    if verbose:
        print(f"\n  케이스별 결과 ──────────────────────────────────────")
        for r in sorted(results, key=lambda x: x.id):
            mark = "O" if r.correct else "X"
            exp = "매칭" if r.expected else "비매칭"
            pred = "매칭" if r.predicted else "비매칭"
            print(f"  [{mark}] {r.id:<10} {r.score:5.1f}점  예상={exp} 예측={pred}  {r.description}")

        failures = [r for r in results if not r.correct]
        if failures:
            print(f"\n  실패 케이스 ({len(failures)}개) ───────────────────────────────")
            for r in failures:
                exp = "매칭" if r.expected else "비매칭"
                pred = "매칭" if r.predicted else "비매칭"
                print(f"  [{r.id}] {r.score:.1f}점 | 예상={exp}, 예측={pred} | {r.description}")

    print(f"{'='*W}\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="분실물/습득물 매칭 벤치마크")
    parser.add_argument(
        "--threshold", type=float, default=DEFAULT_THRESHOLD,
        help=f"분류 임계값 (기본: {DEFAULT_THRESHOLD})",
    )
    parser.add_argument(
        "--find-best", action="store_true",
        help="F1을 최대화하는 임계값을 탐색 후 해당 값으로 리포트 출력",
    )
    parser.add_argument(
        "--verbose", "-v", action="store_true",
        help="케이스별 상세 결과 출력",
    )
    parser.add_argument(
        "--save", metavar="FILE",
        help="결과를 JSON 파일로 저장 (비교용)",
    )
    args = parser.parse_args()

    if args.find_best:
        print("최적 임계값 탐색 중 (20~89 범위)…")
        best = find_best_threshold()
        threshold = best["threshold"]
        print(f"최적 임계값: {threshold}  (F1={best['f1']:.1%})")
    else:
        threshold = args.threshold

    results, metrics = run_benchmark(threshold=threshold)
    print_report(results, metrics, verbose=args.verbose)

    if args.save:
        output = {
            "metrics": metrics,
            "cases": [
                {
                    "id": r.id,
                    "description": r.description,
                    "difficulty": r.difficulty,
                    "expected": r.expected,
                    "score": round(r.score, 2),
                    "predicted": r.predicted,
                    "correct": r.correct,
                }
                for r in sorted(results, key=lambda x: x.id)
            ],
        }
        save_path = Path(args.save)
        save_path.parent.mkdir(parents=True, exist_ok=True)
        with open(save_path, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)
        print(f"결과 저장됨: {save_path}")


if __name__ == "__main__":
    main()
