import os
import time
import copy
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, WeightedRandomSampler
from torchvision import datasets, transforms
import timm
from pathlib import Path
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import json

BASE_DIR   = Path(__file__).parent.parent
DATA_DIR   = BASE_DIR / "data" / "processed"
MODEL_DIR  = BASE_DIR / "models"
MODEL_DIR.mkdir(exist_ok=True)

# ── Hyperparameters ────────────────────────────────────────────────────────────
IMG_SIZE    = 224
BATCH_SIZE  = 32
EPOCHS      = 10
LR          = 1e-4
NUM_WORKERS = 4
DEVICE      = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print(f"🚀 Training on: {DEVICE}")
if DEVICE.type == "cuda":
    print(f"   GPU: {torch.cuda.get_device_name(0)}")

# ── Transforms ─────────────────────────────────────────────────────────────────
train_transforms = transforms.Compose([
    transforms.Resize((IMG_SIZE + 32, IMG_SIZE + 32)),
    transforms.RandomCrop(IMG_SIZE),
    transforms.RandomHorizontalFlip(),
    transforms.RandomVerticalFlip(p=0.2),
    transforms.ColorJitter(brightness=0.3, contrast=0.3, saturation=0.3),
    transforms.RandomRotation(15),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406],
                         [0.229, 0.224, 0.225]),
])

val_transforms = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406],
                         [0.229, 0.224, 0.225]),
])

# ── Dataset ────────────────────────────────────────────────────────────────────
full_dataset = datasets.ImageFolder(DATA_DIR, transform=train_transforms)
print(f"\n📊 Class mapping: {full_dataset.class_to_idx}")
print(f"   Total images : {len(full_dataset)}")

# 80/20 train-val split
val_size   = int(0.2 * len(full_dataset))
train_size = len(full_dataset) - val_size
train_ds, val_ds = torch.utils.data.random_split(
    full_dataset, [train_size, val_size],
    generator=torch.Generator().manual_seed(42)
)
val_ds.dataset = copy.deepcopy(full_dataset)
val_ds.dataset.transform = val_transforms

print(f"   Train samples: {train_size}")
print(f"   Val samples  : {val_size}")

# Handle class imbalance with weighted sampler
targets      = [full_dataset.targets[i] for i in train_ds.indices]
class_counts = [targets.count(i) for i in range(len(full_dataset.classes))]
weights      = [1.0 / class_counts[t] for t in targets]
sampler      = WeightedRandomSampler(weights, num_samples=len(weights), replacement=True)

train_loader = DataLoader(train_ds, batch_size=BATCH_SIZE,
                          sampler=sampler, num_workers=NUM_WORKERS, pin_memory=True)
val_loader   = DataLoader(val_ds,   batch_size=BATCH_SIZE,
                          shuffle=False, num_workers=NUM_WORKERS, pin_memory=True)

# ── Model ──────────────────────────────────────────────────────────────────────
print("\n🧠 Loading EfficientNet-B0 pretrained weights...")
model = timm.create_model("efficientnet_b0", pretrained=True, num_classes=2)
model = model.to(DEVICE)

# Freeze backbone, only train classifier first (faster convergence)
for name, param in model.named_parameters():
    if "classifier" not in name:
        param.requires_grad = False

criterion = nn.CrossEntropyLoss()
optimizer = optim.AdamW(
    filter(lambda p: p.requires_grad, model.parameters()),
    lr=LR, weight_decay=1e-4
)
scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=EPOCHS)

# ── Training loop ──────────────────────────────────────────────────────────────
def train_epoch(model, loader, optimizer, criterion):
    model.train()
    running_loss, correct, total = 0.0, 0, 0
    for inputs, labels in loader:
        inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
        optimizer.zero_grad()
        outputs = model(inputs)
        loss    = criterion(outputs, labels)
        loss.backward()
        optimizer.step()
        running_loss += loss.item() * inputs.size(0)
        _, preds = outputs.max(1)
        correct  += preds.eq(labels).sum().item()
        total    += labels.size(0)
    return running_loss / total, correct / total

def val_epoch(model, loader, criterion):
    model.eval()
    running_loss, correct, total = 0.0, 0, 0
    all_preds, all_labels = [], []
    with torch.no_grad():
        for inputs, labels in loader:
            inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
            outputs        = model(inputs)
            loss           = criterion(outputs, labels)
            running_loss  += loss.item() * inputs.size(0)
            _, preds       = outputs.max(1)
            correct       += preds.eq(labels).sum().item()
            total         += labels.size(0)
            all_preds.extend(preds.cpu().numpy())
            all_labels.extend(labels.cpu().numpy())
    return running_loss / total, correct / total, all_preds, all_labels

# ── Main training ──────────────────────────────────────────────────────────────
best_acc   = 0.0
best_model = None
history    = {"train_loss": [], "val_loss": [], "train_acc": [], "val_acc": []}

print(f"\n🔥 Starting training for {EPOCHS} epochs...\n")

for epoch in range(EPOCHS):
    start = time.time()

    # Unfreeze all layers after epoch 3
    if epoch == 3:
        print("🔓 Unfreezing all layers for fine-tuning...")
        for param in model.parameters():
            param.requires_grad = True
        optimizer = optim.AdamW(model.parameters(), lr=LR * 0.1, weight_decay=1e-4)

    train_loss, train_acc          = train_epoch(model, train_loader, optimizer, criterion)
    val_loss,   val_acc, preds, lbls = val_epoch(model, val_loader, criterion)
    scheduler.step()

    history["train_loss"].append(train_loss)
    history["val_loss"].append(val_loss)
    history["train_acc"].append(train_acc)
    history["val_acc"].append(val_acc)

    elapsed = time.time() - start
    print(f"Epoch [{epoch+1:02d}/{EPOCHS}] "
          f"| Train Loss: {train_loss:.4f} Acc: {train_acc:.4f} "
          f"| Val Loss: {val_loss:.4f} Acc: {val_acc:.4f} "
          f"| Time: {elapsed:.1f}s")

    if val_acc > best_acc:
        best_acc   = val_acc
        best_model = copy.deepcopy(model.state_dict())
        torch.save(best_model, MODEL_DIR / "best_model.pt")
        print(f"   💾 Saved best model (val_acc={best_acc:.4f})")

# ── Final evaluation ───────────────────────────────────────────────────────────
print(f"\n✅ Training complete! Best val accuracy: {best_acc:.4f}")
print("\n📋 Classification Report:")
print(classification_report(lbls, preds,
      target_names=full_dataset.classes))

# Save class mapping
class_info = {
    "class_to_idx": full_dataset.class_to_idx,
    "idx_to_class": {v: k for k, v in full_dataset.class_to_idx.items()},
    "best_val_acc": best_acc
}
with open(MODEL_DIR / "class_info.json", "w") as f:
    json.dump(class_info, f, indent=2)
print(f"💾 Class info saved to models/class_info.json")

# Plot training curves
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
ax1.plot(history["train_loss"], label="Train")
ax1.plot(history["val_loss"],   label="Val")
ax1.set_title("Loss"); ax1.legend(); ax1.set_xlabel("Epoch")
ax2.plot(history["train_acc"], label="Train")
ax2.plot(history["val_acc"],   label="Val")
ax2.set_title("Accuracy"); ax2.legend(); ax2.set_xlabel("Epoch")
plt.tight_layout()
plt.savefig(MODEL_DIR / "training_curves.png", dpi=150)
print(f"📈 Training curves saved to models/training_curves.png")
