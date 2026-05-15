from pathlib import Path

from onnxruntime.quantization import QuantType, quantize_dynamic

model_dir = Path(".onnx/kosimcse/onnx")
input_model = model_dir / "model.onnx"
output_model = model_dir / "model_int8.onnx"

if not input_model.exists():
    raise FileNotFoundError(f"모델 없음: {input_model}")

print(f"양자화 중: {input_model} → {output_model}")
quantize_dynamic(str(input_model), str(output_model), weight_type=QuantType.QInt8)
print("완료")
