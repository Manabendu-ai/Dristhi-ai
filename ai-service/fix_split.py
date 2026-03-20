import json, shutil
from pathlib import Path

raw   = Path("data/raw")
clean = Path("data/processed/clean")
dirty = Path("data/processed/dirty")
clean.mkdir(parents=True, exist_ok=True)
dirty.mkdir(parents=True, exist_ok=True)

ann_path = raw / "annotations.json"
if not ann_path.exists():
    print("❌ No annotations.json in data/raw/")
    exit(1)

with open(ann_path) as f:
    ann = json.load(f)

annotated_ids = set(a["image_id"] for a in ann["annotations"])
print(f"Total images     : {len(ann['images'])}")
print(f"Dirty (annotated): {len(annotated_ids)}")
print(f"Clean            : {len(ann['images']) - len(annotated_ids)}")

dirty_count = clean_count = skipped = 0
for img in ann["images"]:
    fname = f"{img['id']:05d}.jpg"
    src   = raw / fname
    if not src.exists():
        skipped += 1
        continue
    if img["id"] in annotated_ids:
        shutil.copy(src, dirty / fname)
        dirty_count += 1
    else:
        shutil.copy(src, clean / fname)
        clean_count += 1

print(f"\n✅ Done!")
print(f"   dirty/ : {dirty_count}")
print(f"   clean/ : {clean_count}")
print(f"   skipped: {skipped}")
