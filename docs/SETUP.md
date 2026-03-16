# Setup Guide — Astro Mobile App

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog 2023.1.1 or newer |
| JDK | 17+ (Android Studio bundles JBR 21 — use that) |
| Android SDK | API 34 |
| Android NDK | 25.1.8937393 (auto-downloaded by Gradle) |
| CMake | 3.22.1 (auto-downloaded by Gradle) |
| Physical device | API 26+ — sensors and camera are required; emulators will not work |

---

## Getting Started

```bash
git clone https://github.com/DIPE014/Astro-Mobile-App.git
cd Astro-Mobile-App
```

1. Open the project root in Android Studio.
2. Wait for Gradle sync to complete. On first run this downloads the NDK and CMake automatically.
3. Enable Developer Options on your Android device and turn on USB Debugging.
4. Connect the device via USB and confirm the ADB connection prompt.
5. Press Run (or `Shift+F10`).

---

## Build Commands

```bash
# Debug build (outputs to app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Release build (requires a signing keystore configured in build.gradle)
./gradlew assembleRelease

# Install debug APK directly to connected device
./gradlew installDebug

# Run JVM unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Clean build artifacts
./gradlew clean
```

---

## WSL / Windows Build

Building from WSL requires invoking the Windows Gradle wrapper because the Android SDK and NDK are installed under Windows paths:

```bash
# Adjust JAVA_HOME and path to match your installation
cmd.exe /c "set JAVA_HOME=D:\path\to\jdk-17 && cd /d D:\path\to\Astro-Mobile-App && gradlew.bat assembleDebug"
```

Alternatively, build directly in a Windows terminal (PowerShell or cmd):

```bat
cd D:\path\to\Astro-Mobile-App
gradlew.bat assembleDebug
```

---

## Permissions (Requested at Runtime)

| Permission | Purpose |
|------------|---------|
| `CAMERA` | AR live preview + image capture for star detection and stacking |
| `ACCESS_FINE_LOCATION` | Precise GPS for coordinate transforms (Alt/Az, LST) |
| `ACCESS_COARSE_LOCATION` | Fallback when fine location is unavailable |
| `INTERNET` | AstroBot AI chat (OpenAI API calls) |

The app requests each permission at the point it is first needed, with explanatory rationale dialogs. Users who deny camera permission cannot use AR mode, star detection, or image stacking. Users who deny location permission will be prompted to enter their location manually.

---

## AstroBot Setup

AstroBot uses the OpenAI API for AI-assisted astronomy Q&A.

1. Obtain an API key from [platform.openai.com](https://platform.openai.com).
2. Open the app → Settings → AstroBot → enter your API key.
3. The key is stored in `EncryptedSharedPreferences` using AES-256 (Android Keystore-backed).

No API key is needed to use sky map, plate solving, image stacking, or any other offline feature.

---

## NDK Build Notes

The native library (`libastrometry_native.so`) is compiled from approximately 200 C source files from astrometry.net plus two JNI bridge files:

- `app/src/main/cpp/jni/astrometry_jni.c` — star detection and plate solving JNI
- `app/src/main/cpp/jni/stacking_jni.c` — image stacking JNI

**Compile times:**
- First build (NDK + CMake download + full native compile): 5–10 minutes
- Incremental native build: ~30 seconds
- Java-only incremental build: ~10 seconds

**Architectures built:** `arm64-v8a` (modern devices) and `armeabi-v7a` (older 32-bit devices).

**If NDK is not found:**
SDK Manager (in Android Studio) → SDK Tools tab → check "NDK (Side by Side)" → select version 25.1.8937393.

**If CMake is not found:**
SDK Manager → SDK Tools tab → check "CMake" → select version 3.22.1.

---

## Plate Solving Index Files

The FITS index files used by the plate solver are bundled in `app/src/main/assets/indexes/`:

| File | Sky coverage |
|------|-------------|
| index-4115.fits | ~1°–2° fields |
| index-4116.fits | ~2°–4° fields |
| index-4117.fits | ~4°–8° fields |
| index-4118.fits | ~8°–16° fields |
| index-4119.fits | ~16°–32° fields |

These five files cover scale ranges from roughly 10 to 180 arcsec/pixel, adequate for phone camera lenses. On first launch the app copies them from assets to internal storage so the native solver can memory-map them.

For full-sky solving at smaller scales, additional index files (4110–4114, 4200 series) from [astrometry.net](https://astrometry.net/use.html) can be placed in `assets/indexes/` and listed in the solver configuration.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Compass not working / sky drifts | Perform figure-8 calibration: wave the phone in a figure-eight pattern several times |
| Camera permission denied | Settings → Apps → Astro → Permissions → Camera → Allow |
| Build fails: NDK not found | SDK Manager → SDK Tools → NDK (Side by Side) → install 25.1.8937393 |
| Build fails: CMake not found | SDK Manager → SDK Tools → CMake → install 3.22.1 |
| Build fails: JDK version error | Use Android Studio's bundled JBR, or set `JAVA_HOME` to a JDK 17+ installation |
| Tooltip not appearing | Tutorial may already be marked complete in SharedPreferences. Open Settings → Replay Tutorial to reset it |
| Plate solve fails / times out | Ensure the image contains 10+ distinct stars, minimal motion blur, and no bright artificial light sources dominating the frame |
| Stars detected but solve fails | Check that index files are present in `assets/indexes/` and that the scale range (10–180 arcsec/px) matches your lens |
| App crashes on star detection | Verify native library is present: `adb shell ls /data/app/.../lib/arm64/libastrometry_native.so` |

---

## Running WSL Tests (Plate Solving)

The WSL test program `test_solve_wsl.c` runs the full plate-solving pipeline against a reference image on a Linux host, without requiring an Android device. Use this to validate algorithm changes before building the APK.

```bash
# Compile against the app's modified C sources (required — the installed
# libastrometry.so has a bug in the u8+downsample path that will crash)
gcc -o test_solve_wsl test_solve_wsl.c \
    -I/usr/local/astrometry/include -I. \
    -L/usr/local/astrometry/lib \
    -lastrometry -lm -lpthread

./test_solve_wsl
# Expected: SOLVED, RA≈81.37°, Dec≈−0.99°, logodds > 100
```

```bash
# Generate a fresh reference solution with the canonical solve-field tool
cd /mnt/d/Download/DIP
solve-field img.png --downsample 2 --overwrite --no-plots
# Produces img.wcs (reference WCS) and img.axy (reference star list, 677 stars)
```

**Test image:** `/mnt/d/Download/DIP/img.png` — 1920×2560 px, Orion field.
**Reference:** RA = 81.37°, Dec = −0.99° (Orion Nebula region).
**Index files for WSL test:** `/mnt/d/Download/DIP/astrometry-indexes/` (4110–4119).
