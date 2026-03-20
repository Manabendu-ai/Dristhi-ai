import os
import requests
import zipfile
from pathlib import Path
from tqdm import tqdm

BASE    = Path("data")
CLEAN   = BASE / "processed/clean"
DIRTY   = BASE / "processed/dirty"
CLEAN.mkdir(parents=True, exist_ok=True)
DIRTY.mkdir(parents=True, exist_ok=True)

# ── Use the garbage-classification-v2 we already downloaded ──────────────────
# It's in data/raw/standardized_256/ — map ALL classes to dirty
# Pull clean street images from a public dataset via direct URL

print("Step 1: Mapping garbage dataset → dirty/")
raw_256 = BASE / "raw/standardized_256"

if raw_256.exists():
    count = 0
    for cls_folder in sorted(raw_256.iterdir()):
        if not cls_folder.is_dir():
            continue
        imgs = list(cls_folder.glob("*.jpg")) + \
               list(cls_folder.glob("*.png")) + \
               list(cls_folder.glob("*.JPG")) + \
               list(cls_folder.glob("*.PNG"))
        for img in imgs:
            dest = DIRTY / f"{cls_folder.name}_{img.name}"
            if not dest.exists():
                import shutil
                shutil.copy(img, dest)
                count += 1
    print(f"  ✅ {count} dirty images copied")
else:
    print("  ❌ data/raw/standardized_256 not found!")

# ── Download clean street images from MIT Places365 (public) ─────────────────
print("\nStep 2: Downloading clean street images...")

# These are direct links to clean street scene datasets
SOURCES = [
    # GitHub repo with clean street images
    ("https://github.com/opencv/opencv/raw/master/samples/data/lena.jpg", "test.jpg"),
]

# Best approach: use SUN397 subset via direct download
import urllib.request

# Direct download from a verified public source
CLEAN_ZIP_URL = "https://data.vision.ee.ethz.ch/cvl/DIV2K/DIV2K_valid_LR_bicubic_X2.zip"

# Actually let's use a smarter approach — 
# Scrape clean images from Unsplash public API (no auth needed)
print("  Fetching clean street images from public sources...")

clean_queries = [
    "https://source.unsplash.com/400x400/?clean,street,road",
    "https://source.unsplash.com/400x400/?empty,street,pavement",
    "https://source.unsplash.com/400x400/?clean,sidewalk",
    "https://source.unsplash.com/400x400/?park,clean,outdoor",
]

headers = {"User-Agent": "Mozilla/5.0"}
clean_count = 0

# Download 400 clean images (each URL gives random clean street image)
for i in tqdm(range(400)):
    url = clean_queries[i % len(clean_queries)]
    try:
        r = requests.get(url, headers=headers, timeout=15, allow_redirects=True)
        if r.status_code == 200 and len(r.content) > 10000:
            dest = CLEAN / f"clean_{i:04d}.jpg"
            with open(dest, "wb") as f:
                f.write(r.content)
            clean_count += 1
    except Exception as e:
        pass

print(f"  ✅ {clean_count} clean images downloaded")

print(f"\n📊 Final count:")
print(f"   dirty/ : {len(list(DIRTY.glob('*')))}")
print(f"   clean/ : {len(list(CLEAN.glob('*')))}")
print("\n🚀 Ready to train!")
