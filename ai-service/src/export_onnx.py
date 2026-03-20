import torch
import torch.onnx
import timm
import json
from pathlib import Path

MODEL_DIR = Path("models")

print("Loading best model...")
model = timm.create_model("efficientnet_b0", pretrained=False, num_classes=2)
model.load_state_dict(torch.load(MODEL_DIR / "best_model.pt", map_location="cpu"))
model.eval()

dummy_input = torch.randn(1, 3, 224, 224)
onnx_path   = MODEL_DIR / "drishti_model.onnx"

print("Exporting to ONNX...")
torch.onnx.export(
    model, dummy_input, onnx_path,
    export_params=True,
    opset_version=11,
    input_names=["image"],
    output_names=["output"],
    dynamic_axes={"image": {0: "batch_size"}, "output": {0: "batch_size"}}
)
print(f"✅ ONNX model saved to {onnx_path}")
print(f"   Size: {onnx_path.stat().st_size / 1024 / 1024:.1f} MB")
