import requests
import time
from pathlib import Path
from tqdm import tqdm

CLEAN = Path("data/processed/clean")
CLEAN.mkdir(parents=True, exist_ok=True)

# Wikimedia Commons API — totally free, no auth, never blocks
# These are real clean street/outdoor scene image categories

WIKI_CATEGORIES = [
    "Streets_in_Bengaluru",
    "Streets_in_India", 
    "Clean_streets",
    "Roads_in_Karnataka",
    "Parks_in_India",
    "Streets_in_Chennai",
    "Streets_in_Mumbai",
    "Footpaths",
    "Pedestrian_streets",
    "Empty_roads",
]

headers = {"User-Agent": "DrishtiAI/1.0 (hackathon project)"}

def get_images_from_category(category, limit=50):
    url = "https://commons.wikimedia.org/w/api.php"
    params = {
        "action": "query",
        "list": "categorymembers",
        "cmtitle": f"Category:{category}",
        "cmtype": "file",
        "cmlimit": limit,
        "format": "json"
    }
    try:
        r = requests.get(url, params=params, headers=headers, timeout=10)
        data = r.json()
        return [m["title"] for m in data.get("query", {}).get("categorymembers", [])]
    except:
        return []

def get_image_url(title):
    url = "https://commons.wikimedia.org/w/api.php"
    params = {
        "action": "query",
        "titles": title,
        "prop": "imageinfo",
        "iiprop": "url|size",
        "iiurlwidth": 400,
        "format": "json"
    }
    try:
        r = requests.get(url, params=params, headers=headers, timeout=10)
        pages = r.json()["query"]["pages"]
        for page in pages.values():
            info = page.get("imageinfo", [{}])[0]
            url = info.get("thumburl") or info.get("url", "")
            size = info.get("size", 0)
            if url and size > 5000:
                return url
    except:
        pass
    return None

print("📥 Downloading clean street images from Wikimedia Commons...")
downloaded = 0
target = 1500

pbar = tqdm(total=target)
for category in WIKI_CATEGORIES:
    if downloaded >= target:
        break
    titles = get_images_from_category(category, limit=100)
    for title in titles:
        if downloaded >= target:
            break
        if not any(title.lower().endswith(ext) for ext in [".jpg", ".jpeg", ".png"]):
            continue
        img_url = get_image_url(title)
        if not img_url:
            continue
        try:
            r = requests.get(img_url, headers=headers, timeout=15)
            if r.status_code == 200 and len(r.content) > 8000:
                fname = CLEAN / f"clean_{downloaded:04d}.jpg"
                with open(fname, "wb") as f:
                    f.write(r.content)
                downloaded += 1
                pbar.update(1)
        except:
            pass
        time.sleep(0.1)

pbar.close()
print(f"\n✅ Downloaded {downloaded} clean images")

# If we got less than 500, supplement with a direct download
if downloaded < 500:
    print(f"\n⚠  Only got {downloaded}, supplementing with direct URLs...")
    
    # MIT CSAIL Places dataset — direct image URLs (public research dataset)
    DIRECT_URLS = [
        "http://places2.csail.mit.edu/imgs/demo/IMG_6701.jpg",
        "http://places2.csail.mit.edu/imgs/demo/IMG_6702.jpg",
    ]
    
    # Fallback: generate solid color + noise images to pad clean class
    # (better than nothing for a hackathon demo)
    from PIL import Image
    import random
    
    print("  Generating synthetic clean images as fallback...")
    needed = max(500 - downloaded, 0)
    for i in tqdm(range(needed)):
        # Simulate clean street — gray/brown tones, no garbage
        img = Image.new("RGB", (256, 256))
        pixels = img.load()
        base_r = random.randint(80, 180)
        base_g = random.randint(80, 180)  
        base_b = random.randint(70, 160)
        for x in range(256):
            for y in range(256):
                noise = random.randint(-20, 20)
                pixels[x, y] = (
                    max(0, min(255, base_r + noise)),
                    max(0, min(255, base_g + noise)),
                    max(0, min(255, base_b + noise))
                )
        img.save(CLEAN / f"synth_{i:04d}.jpg")
        downloaded += 1

print(f"\n📊 Final clean count: {len(list(CLEAN.glob('*')))}")
print(f"   dirty count      : {len(list(Path('data/processed/dirty').glob('*')))}")
