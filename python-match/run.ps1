$env:MATCH_MODEL_BACKEND = "onnx"
$env:MATCH_ONNX_SAVE_DIR = ".onnx\kosimcse"
$env:MATCH_ONNX_FILE_NAME = "model_int8.onnx"

if (-not $env:CLIP_MODEL_BACKEND) {
    $env:CLIP_MODEL_BACKEND = "onnx-int8"
}
if (-not $env:CLIP_ONNX_MODEL_PATH) {
    $env:CLIP_ONNX_MODEL_PATH = ".onnx\pet-image\pet-image-qint8.onnx"
}
if (-not $env:CLIP_EMBEDDING_MODEL_NAME) {
    $env:CLIP_EMBEDDING_MODEL_NAME = "AvitoTech/Zer0int-CLIP-L-for-animal-identification:onnx-int8"
}

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
