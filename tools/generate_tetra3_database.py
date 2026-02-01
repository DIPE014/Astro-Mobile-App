#!/usr/bin/env python3
"""
Tetra3 Database Generator for Astro-Mobile-App

This script generates the star pattern database used by Tetra3 for
plate solving (identifying stars in photographs).

Requirements:
    pip install git+https://github.com/esa/tetra3.git

    Also need Hipparcos catalog (downloaded automatically or manually):
    wget ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/hip_main.dat
    Copy to: <python-site-packages>/tetra3/hip_main.dat

Usage:
    python generate_tetra3_database.py

Output:
    - hip_database_fov85.npz (in app/src/main/assets/tetra3/)
    - Size: approximately 50-80 MB
    - Generation time: 5-15 minutes

The database is optimized for smartphone cameras with ~65-85 degree FOV.
"""

import os
import sys
import time

def main():
    try:
        import tetra3
    except ImportError:
        print("ERROR: tetra3 not installed")
        print("Install with: pip install git+https://github.com/esa/tetra3.git")
        sys.exit(1)

    # Output directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    output_dir = os.path.join(project_root, "app", "src", "main", "assets", "tetra3")

    # Create output directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)

    output_path = os.path.join(output_dir, "hip_database_fov85")

    print("=" * 60)
    print("Tetra3 Database Generator for Astro-Mobile-App")
    print("=" * 60)
    print()
    print(f"Output: {output_path}.npz")
    print()
    print("Configuration:")
    print("  - Max FOV: 85 degrees (covers most smartphone cameras)")
    print("  - Max magnitude: 7.0 (includes dim stars for better matching)")
    print("  - Star catalog: Hipparcos")
    print()
    print("This will take 5-15 minutes...")
    print()

    start_time = time.time()

    # Create Tetra3 instance
    print("[1/3] Initializing Tetra3...")
    t3 = tetra3.Tetra3()

    # Generate database
    print("[2/3] Generating database (this is the slow part)...")
    print("      Downloading star catalog if not cached...")

    t3.generate_database(
        max_fov=85,                    # Smartphone cameras are 65-85 degrees
        min_fov=10,                    # Support telephoto lenses too
        star_max_magnitude=7.0,        # Include dimmer stars
        pattern_stars_per_fov=10,      # Stars for pattern matching
        verification_stars_per_fov=20, # Stars for verification
        save_as=output_path
    )

    elapsed = time.time() - start_time

    # Check output file
    output_file = output_path + ".npz"
    if os.path.exists(output_file):
        size_mb = os.path.getsize(output_file) / (1024 * 1024)
        print()
        print("[3/3] Done!")
        print()
        print("=" * 60)
        print("SUCCESS")
        print("=" * 60)
        print(f"  File: {output_file}")
        print(f"  Size: {size_mb:.1f} MB")
        print(f"  Time: {elapsed:.1f} seconds")
        print()
        print("The database is ready. Rebuild the Android app:")
        print("  ./gradlew assembleDebug")
    else:
        print()
        print("ERROR: Database file was not created")
        sys.exit(1)


if __name__ == "__main__":
    main()
