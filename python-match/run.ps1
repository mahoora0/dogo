$env:MATCH_MODEL_BACKEND = "onnx"
$env:MATCH_ONNX_SAVE_DIR = ".onnx\kosimcse"
$env:MATCH_ONNX_FILE_NAME = "model_int8.onnx"

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
