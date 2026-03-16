# Developer Documentation — Astro Mobile App

Developer documentation for Astro Mobile App: an Android astronomy app featuring a real-time AR sky map, plate solving, image stacking, AI chat, and offline celestial calculations.

---

## Documents

| File | Description |
|------|-------------|
| [CORE_CONCEPTS.md](CORE_CONCEPTS.md) | Deep dive into every subsystem: coordinate math, orbital mechanics, rendering pipeline, gesture system, plate solving, image stacking, onboarding tooltips |
| [SETUP.md](SETUP.md) | Environment requirements, build commands, NDK notes, troubleshooting, WSL test instructions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Package structure, layering rules, dependency injection, data flow |
| [STRUCTURE.md](STRUCTURE.md) | Full file-tree with per-file descriptions |

---

## Quick Reference

### Build Commands

```bash
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK (requires keystore)
./gradlew installDebug       # Build + install on connected device
./gradlew test               # Run JVM unit tests
./gradlew lint               # Run lint
```

### Key Directories

| Path | Contents |
|------|----------|
| `app/src/main/java/com/astro/app/` | All Java/Kotlin source |
| `app/src/main/cpp/` | Native C sources and JNI bridges |
| `app/src/main/cpp/astrometry/` | astrometry.net C sources (util, solver, libkd, qfits-an, gsl-an) |
| `app/src/main/cpp/jni/` | JNI bridge files (`astrometry_jni.c`, `stacking_jni.c`) |
| `app/src/main/assets/` | Star catalog, constellation data, DSO catalog, education JSON, index FITS files |
| `app/src/test/` | JVM unit tests |
| `docs/` | This documentation |

---

## Feature → Key Files

### Sky Map Rendering
- `app/src/main/java/com/astro/app/core/renderer/SkyCanvasView.java` — main rendering surface, gesture handling, projection
- `app/src/main/java/com/astro/app/ui/skymap/SkyMapActivity.java` — host activity, lifecycle, layer wiring
- `app/src/main/java/com/astro/app/ui/skymap/SkyMapViewModel.java` — state holder for sky map UI

### AR Overlay
- `app/src/main/java/com/astro/app/ui/skymap/AROverlayManager.java` — composites live camera preview with sky canvas
- `app/src/main/java/com/astro/app/ui/skymap/CameraManager.java` — CameraX session management

### Sensor Fusion & Astronomer Model
- `app/src/main/java/com/astro/app/core/control/AstronomerModelImpl.java` — rotation matrix → equatorial pointing
- `app/src/main/java/com/astro/app/core/control/SensorController.java` — accelerometer + magnetometer fusion
- `app/src/main/java/com/astro/app/core/control/LocationController.java` — GPS / network location

### Star Data
- `app/src/main/java/com/astro/app/data/repository/StarRepositoryImpl.java`
- `app/src/main/java/com/astro/app/data/parser/ProtobufParser.java`
- `app/src/main/assets/stars.binary` — Hipparcos catalog (~60,000 stars, Protocol Buffers)

### Constellations
- `app/src/main/java/com/astro/app/core/layers/ConstellationsLayer.java`
- `app/src/main/java/com/astro/app/data/repository/ConstellationRepositoryImpl.java`
- `app/src/main/assets/constellations.binary` — line-segment pairs (star HIP IDs)
- `app/src/main/assets/bound.json`, `bound_18.json`, `bound_edges_18.json`, `bound_in_18.json` — IAU boundary polygons

### Planets
- `app/src/main/java/com/astro/app/core/layers/PlanetsLayer.java`
- `app/src/main/java/com/astro/app/core/control/space/Universe.kt` — `getRaDec(body, date)` for any body/time
- `app/src/main/java/com/astro/app/core/control/space/SolarSystemObject.kt` — Keplerian position computation
- `app/src/main/java/com/astro/app/core/control/OrbitalElements.kt` — J2000.0 elements + century rates

### Deep Sky Objects (Messier)
- `app/src/main/java/com/astro/app/data/repository/MessierRepositoryImpl.java`
- `app/src/main/assets/messier.binary` — 110 Messier objects

### Planet Trajectories
- `app/src/main/java/com/astro/app/core/renderer/SkyCanvasView.java` — trajectory sampling + polyline drawing methods
- `app/src/main/java/com/astro/app/core/control/space/Universe.kt` — position sampling

### Tonight's Highlights
- `app/src/main/java/com/astro/app/core/highlights/TonightsHighlights.java`
- `app/src/main/java/com/astro/app/ui/highlights/TonightsHighlightsFragment.java`

### Time Travel
- `app/src/main/java/com/astro/app/core/control/TimeTravelClock.java`
- `app/src/main/java/com/astro/app/ui/timetravel/TimeTravelDialogFragment.java`

### Search
- `app/src/main/java/com/astro/app/ui/search/SearchActivity.java`
- `app/src/main/java/com/astro/app/search/SearchIndex.java`
- `app/src/main/java/com/astro/app/search/PrefixStore.java`
- `app/src/main/java/com/astro/app/search/SearchArrowView.java`

### AstroBot AI Chat
- `app/src/main/java/com/astro/app/ui/chat/ChatBottomSheetFragment.java`
- `app/src/main/java/com/astro/app/ui/chat/ChatViewModel.java`
- `app/src/main/java/com/astro/app/data/api/OpenAIClient.java`

### Image Stacking
- `app/src/main/java/com/astro/app/ui/stacking/ImageStackingActivity.java`
- `app/src/main/java/com/astro/app/native_/ImageStackingManager.java`
- `app/src/main/java/com/astro/app/native_/StackingNative.java`
- `app/src/main/cpp/jni/stacking_jni.c` — triangle matching, RANSAC affine, bilinear warp, mean accumulator

### Plate Solving
- `app/src/main/java/com/astro/app/native_/NativePlateSolver.java`
- `app/src/main/java/com/astro/app/native_/AstrometryNative.java`
- `app/src/main/cpp/jni/astrometry_jni.c` — star detection + solver JNI bridge
- `app/src/main/assets/indexes/` — FITS index files 4115–4119
- `app/src/main/java/com/astro/app/ui/platesolve/PlateSolveActivity.java`

### Star Information
- `app/src/main/java/com/astro/app/ui/starinfo/StarInfoActivity.java`
- `app/src/main/java/com/astro/app/ui/starinfo/StarInfoViewModel.java`
- `app/src/main/java/com/astro/app/data/education/EducationRepository.java`

### Sky Brightness (Bortle Scale)
- `app/src/main/java/com/astro/app/ui/skybrightness/SkyBrightnessActivity.java`
- `app/src/main/java/com/astro/app/ui/skybrightness/SkyBrightnessAnalyzer.java`
- `app/src/main/java/com/astro/app/ui/skybrightness/BortleScaleView.java`

### Onboarding Tooltips
- `app/src/main/java/com/astro/app/ui/onboarding/TooltipManager.java`
- `app/src/main/java/com/astro/app/ui/onboarding/TooltipView.java`
- `app/src/main/java/com/astro/app/ui/onboarding/TooltipConfig.java`
- `app/src/main/java/com/astro/app/ui/onboarding/OnboardingActivity.java`

### Intro Splash
- `app/src/main/java/com/astro/app/IntroSplashActivity.java`
- `app/src/main/java/com/astro/app/ui/intro/StarFieldView.java` — animated particle star field

### Constellation Identification
- `app/src/main/java/com/astro/app/ui/skymap/ConstellationBoundaryResolver.java`
- `app/src/main/assets/bound.json`, `bound_18.json` — IAU polygon boundaries

### Settings
- `app/src/main/java/com/astro/app/ui/settings/SettingsActivity.java`
- `app/src/main/java/com/astro/app/ui/settings/SettingsViewModel.java`

### Education Content
- `app/src/main/java/com/astro/app/data/education/EducationRepository.java`
- `app/src/main/java/com/astro/app/ui/education/EducationDetailActivity.java`
- `app/src/main/assets/education_content.json` — star, constellation, and DSO descriptions
- `app/src/main/assets/planets_education.json` — planet fact sheets

### Coordinate Math
- `app/src/main/java/com/astro/app/core/math/CoordinateManipulations.kt` — RA/Dec ↔ Alt/Az, hour angle
- `app/src/main/java/com/astro/app/core/math/Astronomy.kt` — GMST, LST, Julian date
- `app/src/main/java/com/astro/app/core/math/Vector3.kt` — 3D vector operations
- `app/src/main/java/com/astro/app/core/math/RaDec.kt` — RA/Dec value type and unit vector conversion
- `app/src/main/java/com/astro/app/core/math/Matrix3x3.kt` — rotation matrix operations
- `app/src/main/java/com/astro/app/core/math/TimeUtils.kt` — Julian date conversion, sidereal time

### Dependency Injection
- `app/src/main/java/com/astro/app/di/AppComponent.java` — Dagger component
- `app/src/main/java/com/astro/app/di/AppModule.java` — singleton bindings

---

## Testing

Unit tests live in `app/src/test/java/com/astro/app/`. Run with `./gradlew test`.

| Test file | Coverage |
|-----------|---------|
| `core/math/Matrix3x3Test.java` | Rotation matrix multiply, transpose, determinant |
| `core/math/RaDecTest.java` | RA/Dec ↔ unit vector round-trip |
| `core/math/TimeUtilsTest.java` | Julian date, GMST, LST calculations |
| `core/math/Vector3Test.java` | Dot product, cross product, normalization |
| `data/model/GeocentricCoordsTest.java` | Geocentric coordinate model |
| `data/model/StarDataTest.java` | StarData parsing and field access |

---

## Native Layer Reference

The native library (`libastrometry_native.so`) is built from:

| Source group | Location | Purpose |
|-------------|----------|---------|
| astrometry.net util | `app/src/main/cpp/astrometry/util/` | simplexy, image2xy, dsigma, ctmf, dallpeaks |
| astrometry.net solver | `app/src/main/cpp/astrometry/solver/` | solver, verify, tweak2, fit-wcs, index |
| libkd | `app/src/main/cpp/astrometry/libkd/` | K-d tree (used by solver and stacking) |
| qfits-an | `app/src/main/cpp/astrometry/qfits-an/` | FITS file I/O for index files |
| gsl-an | `app/src/main/cpp/astrometry/gsl-an/` | Linear algebra (LU solve for RANSAC) |
| JNI bridges | `app/src/main/cpp/jni/` | `astrometry_jni.c`, `stacking_jni.c` |

**Modified upstream files:**
- `astrometry/util/image2xy.c` line 68: added `s->image_u8 = NULL;` after upconvert — fixes assertion crash on u8+downsample path (upstream bug)
- `astrometry/solver/verify.c`: reverted to upstream (no local modifications)

For algorithm details see [CORE_CONCEPTS.md §14](CORE_CONCEPTS.md#14-image-stacking-pipeline) (stacking) and [§15](CORE_CONCEPTS.md#15-plate-solving-pipeline) (plate solving).
