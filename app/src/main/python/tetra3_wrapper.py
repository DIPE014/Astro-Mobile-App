"""
Tetra3 wrapper for plate solving star images from Android.

This module provides the interface between the Android app and the Tetra3
star pattern matching algorithm.
"""

import tetra3
import numpy as np
from PIL import Image
import io
import json

_solver = None


def initialize(database_path):
    """
    Initialize the Tetra3 solver with the star database.

    Args:
        database_path: Path to the .npz database file

    Returns:
        True if successful, False otherwise
    """
    global _solver
    try:
        _solver = tetra3.Tetra3(database_path)
        return True
    except Exception as e:
        print(f"Failed to initialize Tetra3: {e}")
        return False


def solve_image(image_bytes, fov_hint=70):
    """
    Solve a star field image to determine sky coordinates.

    Args:
        image_bytes: JPEG/PNG image as bytes
        fov_hint: Estimated field of view in degrees (default 70)

    Returns:
        JSON string with solve results:
        - status: SUCCESS, NO_MATCH, NOT_ENOUGH_STARS, or ERROR
        - centerRa: Right ascension of image center (degrees)
        - centerDec: Declination of image center (degrees)
        - fov: Actual field of view (degrees)
        - roll: Image rotation angle (degrees)
        - matchedStars: List of matched stars with hipId, pixelX, pixelY
    """
    global _solver
    if _solver is None:
        return json.dumps({
            "status": "ERROR",
            "message": "Database not loaded. Call initialize() first."
        })

    try:
        # Load and convert image to grayscale
        img = Image.open(io.BytesIO(image_bytes)).convert('L')
        img_array = np.array(img)

        # Extract star centroids from the image
        centroids = tetra3.get_centroids_from_image(
            img_array,
            sigma=2.5,
            filtsize=25,
            min_area=4,
            max_area=500
        )

        if len(centroids) < 4:
            return json.dumps({
                "status": "NOT_ENOUGH_STARS",
                "starsDetected": len(centroids),
                "message": f"Only {len(centroids)} stars detected, need at least 4"
            })

        # Attempt to solve the star pattern
        solution = _solver.solve_from_centroids(
            centroids,
            size=(img_array.shape[1], img_array.shape[0]),
            fov_estimate=fov_hint,
            fov_max_error=15
        )

        # Check if solve was successful
        if solution['RA'] is None:
            return json.dumps({
                "status": "NO_MATCH",
                "starsDetected": len(centroids),
                "message": "Could not match star pattern to database"
            })

        # Build list of matched stars
        matched = []
        matched_ids = solution.get('matched_catID', [])
        matched_centroids = solution.get('matched_centroids', centroids)

        for i, hip_id in enumerate(matched_ids):
            if i < len(matched_centroids):
                matched.append({
                    "hipId": int(hip_id),
                    "pixelX": float(matched_centroids[i][0]),
                    "pixelY": float(matched_centroids[i][1])
                })

        return json.dumps({
            "status": "SUCCESS",
            "centerRa": float(solution['RA']),
            "centerDec": float(solution['Dec']),
            "fov": float(solution['FOV']),
            "roll": float(solution['Roll']),
            "matchedStars": matched,
            "totalStarsDetected": len(centroids),
            "starsMatched": len(matched)
        })

    except Exception as e:
        return json.dumps({
            "status": "ERROR",
            "message": str(e)
        })


def get_version():
    """Return version info for debugging."""
    return json.dumps({
        "tetra3": tetra3.__version__ if hasattr(tetra3, '__version__') else "unknown",
        "wrapper": "1.0.0"
    })
