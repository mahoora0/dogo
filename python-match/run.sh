#!/bin/bash
export MATCH_MODEL_BACKEND=onnx
export MATCH_ONNX_SAVE_DIR=.onnx/kosimcse
export MATCH_ONNX_FILE_NAME=model_int8.onnx
export MATCH_ONNX_EXPORT=true

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
