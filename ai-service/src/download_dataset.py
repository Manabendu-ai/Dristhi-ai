import os
import json
import shutil
import requests
from tqdm import tqdm
from pathlib import Path

BASE_DIR = Path(__file__).parent.parent
RAW_DIR = BASE_DIR / "data" / "raw"
PROCESSED_DIR = BASE_DIR / "data" / "processed"
CLEAN_DIR = PROCESSED_DIR / "clean"
DIRTY_DIR = PROCESSED_DIR / "dirty"

TACO_ANNOTATIONS_URL = "https://raw.githubusercontent.com/pedropro/TACO/master/data/annotations.json"

def download_file(url, dest_path):
    response = requests.get(url, stream=True)
    total = int(response.headers.get("content-length", 0))
    with open(dest_path, "wb") as f, tqdm(
        desc=str(dest_path.name), total=total,
        unit="B", unit_scale=True, unit_divisor=1024
    ) as bar:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)
            bar.update(len(chunk))

def download_taco_images(annotations, raw_dir):
    images = annotations["images"]
    print(f"\n📦 Downloading {len(images)} TACO images...")
    for img in tqdm(images):
        url = img["flickr_url"]
        fname = f"{img['id']:05d}.jpg"
        dest = raw_dir / fname
        if dest.exists():
            continue
        try:
            r = requests.get(url, timeout=10)
            if r.status_code == 200:
                with open(dest, "wb") as f:
                    f.write(r.content)
        except Exception as e:
            print(f"  ⚠ Skipped {fname}: {e}")

def split_clean_dirty(annotations, raw_dir):
    print("\n🗂 Splitting into clean / dirty...")
    # images WITH annotations = dirty (garbage present)
    annotated_ids = set(ann["image_id"] for ann in annotations["annotations"])
    images = annotations["images"]

    dirty_count, clean_count = 0, 0
    for img in tqdm(images):
        fname = f"{img['id']:05d}.jpg"
        src = raw_dir / fname
        if not src.exists():
            continue
        if img["id"] in annotated_ids:
            shutil.copy(src, DIRTY_DIR / fname)
            dirty_count += 1
        else:
            shutil.copy(src, CLEAN_DIR / fname)
            clean_count += 1

    print(f"\n✅ Done!")
    print(f"   🗑  Dirty images : {dirty_count}")
    print(f"   ✨ Clean images  : {clean_count}")

def add_clean_street_images():
    """
    Pull extra clean street images from Open Images v7
    so the clean class isn't too small.
    """
    print("\n🌆 Fetching extra clean street images from Open Images...")
    OPEN_IMAGES_URLS = [
        "https://farm5.staticflickr.com/4458/37056894890_8b7a1cd07b_z.jpg",
        "https://farm5.staticflickr.com/4453/36992637140_8d2f22f474_z.jpg",
        "https://farm5.staticflickr.com/4455/36992634850_0c72e3b33b_z.jpg",
        "https://farm5.staticflickr.com/4464/36992632000_0aca3e4cc4_z.jpg",
        "https://farm1.staticflickr.com/865/41519041225_a4b1b5bce5_z.jpg",
    ]
    for i, url in enumerate(tqdm(OPEN_IMAGES_URLS)):
        try:
            r = requests.get(url, timeout=10)
            if r.status_code == 200:
                with open(CLEAN_DIR / f"open_clean_{i:04d}.jpg", "wb") as f:
                    f.write(r.content)
        except Exception as e:
            print(f"  ⚠ Skipped {i}: {e}")

if __name__ == "__main__":
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    CLEAN_DIR.mkdir(parents=True, exist_ok=True)
    DIRTY_DIR.mkdir(parents=True, exist_ok=True)

    # Download annotations
    ann_path = RAW_DIR / "annotations.json"
    if not ann_path.exists():
        print("📥 Downloading TACO annotations...")
        download_file(TACO_ANNOTATIONS_URL, ann_path)

    with open(ann_path) as f:
        annotations = json.load(f)

    print(f"📊 Dataset stats:")
    print(f"   Images     : {len(annotations['images'])}")
    print(f"   Annotations: {len(annotations['annotations'])}")
    print(f"   Categories : {len(annotations['categories'])}")

    download_taco_images(annotations, RAW_DIR)
    split_clean_dirty(annotations, RAW_DIR)
    add_clean_street_images()
