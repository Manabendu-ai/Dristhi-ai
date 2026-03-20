from satellite.routes import router as satellite_router
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import torch
import timm
from torchvision import transforms
from PIL import Image
import io
import json
import base64
import time
from pathlib import Path

app = FastAPI(
    title="DRISHTI AI Service",
    description="EfficientNet-B0 image classifier — CLEAN vs DIRTY street detection for BBMP Bengaluru",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(satellite_router)

# ── Load model ─────────────────────────────────────────────────────────────────
MODEL_DIR  = Path("models")
DEVICE     = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print(f"🚀 Loading model on {DEVICE}...")
model = timm.create_model("efficientnet_b0", pretrained=False, num_classes=2)
model.load_state_dict(torch.load(MODEL_DIR / "best_model.pt", map_location=DEVICE))
model.eval()
model.to(DEVICE)

with open(MODEL_DIR / "class_info.json") as f:
    class_info = json.load(f)

IDX_TO_CLASS = class_info["idx_to_class"]
print(f"✅ Model loaded! Classes: {IDX_TO_CLASS}")
print(f"   Best val accuracy: {class_info['best_val_acc']:.4f}")

# ── Transforms ─────────────────────────────────────────────────────────────────
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406],
                         [0.229, 0.224, 0.225]),
])

# ── Schemas ────────────────────────────────────────────────────────────────────
class PredictResponse(BaseModel):
    label:       str
    confidence:  float
    clean_prob:  float
    dirty_prob:  float
    verdict:     str
    model_ver:   str
    latency_ms:  float

class HealthResponse(BaseModel):
    status:    str
    device:    str
    model:     str
    accuracy:  float

# ── Inference helper ───────────────────────────────────────────────────────────
def predict_image(img: Image.Image) -> PredictResponse:
    start     = time.time()
    tensor    = transform(img.convert("RGB")).unsqueeze(0).to(DEVICE)

    with torch.no_grad():
        logits = model(tensor)
        probs  = torch.softmax(logits, dim=1)[0]

    clean_prob = float(probs[class_info["class_to_idx"]["clean"]])
    dirty_prob = float(probs[class_info["class_to_idx"]["dirty"]])
    label      = "clean" if clean_prob > dirty_prob else "dirty"
    confidence = max(clean_prob, dirty_prob)
    latency    = (time.time() - start) * 1000

    if label == "clean" and confidence > 0.75:
        verdict = "VERIFIED_CLEAN ✅"
    elif label == "dirty" and confidence > 0.75:
        verdict = "VERIFIED_DIRTY ❌"
    else:
        verdict = "UNCERTAIN — manual review needed ⚠️"

    return PredictResponse(
        label=label,
        confidence=round(confidence, 4),
        clean_prob=round(clean_prob, 4),
        dirty_prob=round(dirty_prob, 4),
        verdict=verdict,
        model_ver="efficientnet_b0_v1",
        latency_ms=round(latency, 2)
    )

# ── Routes ─────────────────────────────────────────────────────────────────────
@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="ok",
        device=str(DEVICE),
        model="efficientnet_b0",
        accuracy=round(class_info["best_val_acc"], 4)
    )

@app.post("/predict", response_model=PredictResponse)
async def predict_upload(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(400, "File must be an image")
    contents = await file.read()
    img = Image.open(io.BytesIO(contents))
    return predict_image(img)

@app.post("/predict/base64", response_model=PredictResponse)
async def predict_base64(payload: dict):
    try:
        img_data = payload.get("image", "")
        if "," in img_data:
            img_data = img_data.split(",")[1]
        img = Image.open(io.BytesIO(base64.b64decode(img_data)))
        return predict_image(img)
    except Exception as e:
        raise HTTPException(400, f"Invalid image data: {e}")

@app.post("/predict/url", response_model=PredictResponse)
async def predict_url(payload: dict):
    import requests as req
    url = payload.get("url", "")
    if not url:
        raise HTTPException(400, "url field required")
    try:
        r   = req.get(url, timeout=10)
        img = Image.open(io.BytesIO(r.content))
        return predict_image(img)
    except Exception as e:
        raise HTTPException(400, f"Could not fetch image: {e}")

@app.get("/")
def root():
    return {
        "service": "DRISHTI AI Service",
        "description": "Clean vs Dirty street image classifier",
        "model": "EfficientNet-B0",
        "accuracy": f"{class_info['best_val_acc']:.2%}",
        "device": str(DEVICE),
        "endpoints": ["/health", "/predict", "/predict/base64", "/predict/url", "/docs"]
    }
