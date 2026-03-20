from pathlib import Path
import shutil, random
from PIL import Image, ImageFilter, ImageEnhance
from tqdm import tqdm

DIRTY = Path("data/processed/dirty")
CLEAN = Path("data/processed/clean")
CLEAN.mkdir(parents=True, exist_ok=True)

dirty_imgs = list(DIRTY.glob("*.jpg")) + list(DIRTY.glob("*.png"))
random.seed(42)

CLEAN_CLASSES = ["green-glass", "white-glass", "brown-glass", "cardboard", "paper"]
clean_sources = [
    p for p in dirty_imgs
    if any(p.name.startswith(cls) for cls in CLEAN_CLASSES)
]

print(f"Source images for clean class: {len(clean_sources)}")
print(f"Generating 2000 clean images via augmentation...")

count = 0
target = 2000
random.shuffle(clean_sources)
idx = 0

with tqdm(total=target) as pbar:
    while count < target:
        src = clean_sources[idx % len(clean_sources)]
        idx += 1
        try:
            img = Image.open(src).convert("RGB").resize((256, 256))
            aug = random.randint(0, 5)
            if aug == 0:
                img = img.filter(ImageFilter.GaussianBlur(radius=1))
            elif aug == 1:
                img = ImageEnhance.Brightness(img).enhance(random.uniform(0.8, 1.3))
            elif aug == 2:
                img = img.rotate(random.randint(-30, 30))
            elif aug == 3:
                img = ImageEnhance.Contrast(img).enhance(random.uniform(0.8, 1.2))
            elif aug == 4:
                img = img.transpose(Image.FLIP_LEFT_RIGHT)
            elif aug == 5:
                w, h = img.size
                left = random.randint(0, 30)
                top  = random.randint(0, 30)
                img  = img.crop((left, top, w, h)).resize((256, 256))
            img.save(CLEAN / f"clean_{count:04d}.jpg", quality=85)
            count += 1
            pbar.update(1)
        except Exception as e:
            pass

print(f"\n✅ Done!")
print(f"   dirty/ : {len(list(DIRTY.glob('*')))}")
print(f"   clean/ : {len(list(CLEAN.glob('*')))}")
print("\n🚀 Run train.py now!")
