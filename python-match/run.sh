#!/bin/bash
export MATCH_MODEL_BACKEND=onnx
export MATCH_ONNX_SAVE_DIR=.onnx/kosimcse
export MATCH_ONNX_FILE_NAME=model_int8.onnx
export MATCH_ONNX_EXPORT=true
export CLIP_ONNX_ENABLED=${CLIP_ONNX_ENABLED:-false}
export CLIP_ONNX_MODEL_PATH=${CLIP_ONNX_MODEL_PATH:-.onnx/pet-image/pet-image-qint8.onnx}
if [ -z "$CLIP_EMBEDDING_MODEL_NAME" ] && { [ "$CLIP_ONNX_ENABLED" = "1" ] || [ "$CLIP_ONNX_ENABLED" = "true" ] || [ "$CLIP_ONNX_ENABLED" = "yes" ] || [ "$CLIP_ONNX_ENABLED" = "on" ]; }; then
  export CLIP_EMBEDDING_MODEL_NAME=AvitoTech/Zer0int-CLIP-L-for-animal-identification:onnx-int8
fi

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
