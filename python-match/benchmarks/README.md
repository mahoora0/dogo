# Python Match Benchmark

This folder contains a small labeled benchmark for the semantic matcher.

Run it from the `python-match` directory:

```bash
uv run python benchmarks/run_benchmark.py
```

Useful variants:

```bash
uv run python benchmarks/run_benchmark.py --json
uv run python benchmarks/run_benchmark.py --threshold 55
uv run python benchmarks/run_benchmark.py --fail-under-top1 0.75 --fail-under-topk 0.90
```

After the model is cached locally, you can avoid Hugging Face network checks:

```bash
HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1 uv run python benchmarks/run_benchmark.py
```

Metrics:

- `Top-1 accuracy`: the expected found item is the highest-scoring candidate.
- `Recall@K`: the expected found item appears within the top K candidates.
- `MRR`: rewards the expected item appearing near the top.
- `Mean positive-hard-negative gap`: how far the correct item scores above the strongest hard negative.
- `Threshold precision/recall/F1`: candidate-level metrics if scores at or above the threshold are treated as matches.

When changing prompt text construction, model choice, or Java/Python score weights, run the same dataset before and after the change and compare these numbers. Add new cases whenever you find a real false positive or false negative in the app.
