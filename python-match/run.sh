#!/bin/bash
export MATCH_MODEL_BACKEND=onnx
export MATCH_ONNX_SAVE_DIR=.onnx/kosimcse
export MATCH_ONNX_FILE_NAME=model_int8.onnx
export MATCH_ONNX_EXPORT=true
export CLIP_MODEL_BACKEND=${CLIP_MODEL_BACKEND:-onnx-int8}
export CLIP_ONNX_MODEL_PATH=${CLIP_ONNX_MODEL_PATH:-.onnx/pet-image/pet-image-qint8.onnx}
export CLIP_EMBEDDING_MODEL_NAME=${CLIP_EMBEDDING_MODEL_NAME:-AvitoTech/Zer0int-CLIP-L-for-animal-identification:onnx-int8}

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
