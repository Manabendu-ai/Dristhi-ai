from fastapi import APIRouter, UploadFile, File, HTTPException
from pathlib import Path
import json
import tempfile
import os

from satellite.hotspot_detector import SatelliteHotspotDetector

router   = APIRouter(prefix="/satellite", tags=["Satellite Analysis"])
detector = SatelliteHotspotDetector()

@router.get("/hotspots")
def get_hotspots():
    """Get satellite-derived waste hotspot map for all Bengaluru wards"""
    return {
        "success": True,
        "source":  "IRS Resourcesat-2A LISS-IV (5.8m resolution)",
        "method":  "Supervised classification — Maximum Likelihood",
        "hotspots": detector.generate_full_hotspot_map()
    }

@router.get("/hotspots/priority")
def get_priority(top: int = 5):
    """Get top N priority wards needing immediate cleanup"""
    return {
        "success":        True,
        "priority_wards": detector.get_priority_wards(top),
        "message":        f"Top {top} wards requiring immediate BBMP intervention"
    }

@router.get("/hotspots/geojson")
def get_geojson():
    """GeoJSON export for frontend map rendering"""
    return detector.export_geojson()

@router.post("/upload/shapefile")
async def upload_shapefile(file: UploadFile = File(...)):
    """
    Upload QGIS shapefile (.shp or .zip with .shp/.dbf/.shx)
    This is called when hackathon dataset is provided
    """
    suffix = Path(file.filename).suffix.lower()
    if suffix not in [".shp", ".zip", ".geojson"]:
        raise HTTPException(400, "Upload .shp, .zip, or .geojson file")

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    result = detector.load_shapefile(tmp_path)
    os.unlink(tmp_path)

    if result is not None:
        return {
            "success":  True,
            "message":  "Shapefile loaded successfully",
            "features": len(result),
            "columns":  list(result.columns),
            "crs":      str(result.crs)
        }
    raise HTTPException(500, "Failed to load shapefile")

@router.post("/upload/raster")
async def upload_raster(file: UploadFile = File(...)):
    """
    Upload satellite raster (GeoTIFF from IRS/Sentinel)
    Called when hackathon satellite imagery is provided
    """
    suffix = Path(file.filename).suffix.lower()
    if suffix not in [".tif", ".tiff"]:
        raise HTTPException(400, "Upload GeoTIFF (.tif) file")

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    result = detector.load_raster(tmp_path)
    os.unlink(tmp_path)

    if result is not None:
        return {
            "success": True,
            "message": "Raster loaded — running spectral analysis...",
            "metadata": str(result)
        }
    raise HTTPException(500, "Failed to load raster")

@router.get("/satellite/info")
def satellite_info():
    """Info about satellite data sources used by DRISHTI"""
    return {
        "primary_satellite":  "IRS Resourcesat-2A",
        "sensor":             "LISS-IV (Linear Imaging Self Scanner)",
        "resolution":         "5.8 metres",
        "revisit_time":       "5 days",
        "bands": {
            "Band2": "Green  (0.52–0.59 μm) — vegetation health",
            "Band3": "Red    (0.62–0.68 μm) — soil/waste detection",
            "Band4": "NIR    (0.77–0.86 μm) — urban heat signature",
        },
        "classification_method": "Supervised Maximum Likelihood",
        "application": "Persistent waste hotspot detection across BBMP wards",
        "reference": "Bootcamp: Remote Sensing & Space Imaging for Solid Waste"
    }
