# Architecture

## Overview

Astro-Mobile-App is an Android astronomy application written in Java and Kotlin. It provides AR sky rendering driven by live device sensors, native astrometry plate solving compiled from astrometry.net C sources via JNI, AI-powered chat through the OpenAI API, and multi-frame image stacking with triangle asterism alignment. The architecture follows MVVM with Dagger 2 dependency injection. Min SDK is 26 (Android 8), target SDK is 34.

---

## Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer                      │
│  Activities · Fragments · Custom Views · Adapters   │
│  SkyMapActivity · ImageStackingActivity · ChatSheet  │
│  SkyCanvasView · StarFieldView · BortleScaleView     │
├─────────────────────────────────────────────────────┤
│              ViewModel Layer                         │
│  SkyMapViewModel · ChatViewModel · SettingsViewModel │
│  StarInfoViewModel · Dagger 2 DI                    │
├─────────────────────────────────────────────────────┤
│              Domain / Business Logic Layer           │
│  AstronomerModel · Universe · Layers · Math          │
│  AstroBot · SkyBrightnessAnalyzer · TonightsHighlights│
│  NativePlateSolver · ImageStackingManager            │
├─────────────────────────────────────────────────────┤
│              Data Layer                              │
│  Repositories · ProtobufParser · OpenAIClient        │
│  AssetDataSource · EducationRepository               │
├─────────────────────────────────────────────────────┤
│              Native Layer (JNI / C)                  │
│  astrometry_jni.c · stacking_jni.c                  │
│  astrometry.net C sources · gsl-an · libkd           │
└─────────────────────────────────────────────────────┘
```

---

## Package-by-Package Reference

### `ui/skymap/` — Main AR Activity

The primary screen of the app. Hosts the live camera preview behind a sky canvas overlay.

| File | Purpose |
|------|---------|
| `SkyMapActivity.java` | Camera + motion sensors + sky rendering. Draggable FAB radial menu (MotionLayout). Manual pan mode toggle. AR overlay toggle. Time travel. Night mode. Circular reveal transition into the activity. |
| `SkyMapViewModel.java` | MVVM state: night mode on/off, layer visibility flags (stars, planets, constellations, grid, DSOs), magnitude limit slider value. |
| `AROverlayManager.java` | Reads the physical camera FOV from CameraX and calibrates the sky canvas view to match it, so rendered objects align with the real sky. |
| `CameraManager.java` | CameraX lifecycle management. Provides a preview surface for AR mode and an `ImageCapture` use-case for plate solving and stacking. |
| `CameraPermissionHandler.java` | Wraps `ActivityResultLauncher` for CAMERA permission request. |
| `ConstellationBoundaryResolver.java` | Given a sky coordinate, walks the IAU boundary JSON to resolve which constellation name it falls in. |
| `SkyOverlayView.java` | Transparent overlay view drawn on top of the camera surface for AR UI elements (reticle, chip strip). |

### `ui/onboarding/` — Tutorial System

A two-tier onboarding system: a pageable intro walkthrough plus per-screen tooltip sequences.

| File | Purpose |
|------|---------|
| `OnboardingActivity.java` | 6-page ViewPager2 intro shown on first launch. Navigates to SkyMapActivity on completion. |
| `OnboardingPagerAdapter.java` | Supplies onboarding page fragments to the ViewPager2. |
| `TooltipManager.java` | Orchestrates tooltip sequences on any screen. Stores per-screen completion state in SharedPreferences (keys: `KEY_SKYMAP_TUTORIAL`, `KEY_CHAT_TUTORIAL`, `KEY_DETECT_TUTORIAL`, `KEY_SETTINGS_TUTORIAL`, `KEY_SEARCH_TUTORIAL`). Supports a custom `rootView` parameter so tooltips work correctly inside dialogs and nested containers. Corrects anchor coordinates from window-space to rootView-relative space. |
| `TooltipView.java` | Custom full-screen overlay view. Renders a semi-transparent scrim with a punch-through highlight circle around the anchor. Displays step counter, Skip / Next / Got it buttons. Claims `ACTION_DOWN` for gesture blocking but passes taps through to anchor views in interactive mode. |
| `TooltipConfig.java` | Immutable configuration built via the Builder pattern. Positions: `ABOVE`, `BELOW`, `LEFT`, `RIGHT`, `CENTER` with automatic fallback if the preferred position would place the tooltip off-screen. Supports interactive mode (tap-through), extra highlight views, and an `onShowAction` callback. |

### `ui/intro/` — Animated Splash Screen

| File | Purpose |
|------|---------|
| `StarFieldView.java` | Custom `View` rendering 220 animated stars with parallax depth, periodic meteor spawning, and a warp-tunnel zoom effect. Fires a `warpCompleteListener` callback after the tunnel animation completes. If the view is detached at callback time, delivery is deferred until `onAttachedToWindow`. |
| `IntroSplashActivity.java` | Hosts `StarFieldView`. On warp completion, transitions to `SkyMapActivity` with a circular reveal animation (or a first-launch onboarding flow). |

Note: `IntroSplashActivity.java` lives at the root package `com.astro.app` (not `ui/intro/`) due to a historical layout, but its companion view `StarFieldView.java` is in `ui/intro/`.

### `ui/stacking/` — Detect and Stack

| File | Purpose |
|------|---------|
| `ImageStackingActivity.java` | Unified UI for single-frame plate solving and multi-frame stacking. Camera capture via `ActivityResultContracts.TakePicture`. Gallery picker for loading saved images. `MAX_PROCESSING_DIMENSION = 4096` caps input before native processing. Adaptive star detection with automatic `plim` retry if fewer than 30 stars are returned. Shows photography tips dialog and example capture dialog on first use. Handles cancellation mid-solve. |

### `ui/platesolve/` — Legacy Plate Solve UI

| File | Purpose |
|------|---------|
| `PlateSolveActivity.java` | Earlier standalone plate solve activity. Superseded by `ImageStackingActivity` but retained for direct navigation. |

### `ui/chat/` — AstroBot AI Assistant

| File | Purpose |
|------|---------|
| `ChatBottomSheetFragment.java` | `BottomSheetDialogFragment`. Receives context about the currently selected sky object and includes it in the system prompt. Delays tooltip start by 300 ms (removed with `removeCallbacks` in `onDestroyView` to prevent leaks). |
| `ChatViewModel.java` | Holds message list as `LiveData<List<ChatMessage>>`. Calls `OpenAIClient` and posts streaming tokens to the UI thread. |
| `ChatMessageAdapter.java` | `RecyclerView.Adapter` with two view types: user bubble and bot bubble. |

### `ui/skybrightness/` — Light Pollution Meter

| File | Purpose |
|------|---------|
| `SkyBrightnessActivity.java` | Hosts the camera stream and Bortle gauge. |
| `SkyBrightnessAnalyzer.java` | Processes `ImageProxy` frames from CameraX analysis use-case. Computes mean luminance and maps to Bortle scale 1–9. |
| `BortleScaleView.java` | Custom arc gauge view rendering the Bortle classification (1 = darkest, 9 = inner city). |

### `ui/search/` — Object Search

| File | Purpose |
|------|---------|
| `SearchActivity.java` | Text search over stars, constellations, and Messier objects. Drives `SearchArrowView` to point toward the found object. |
| `SearchResultAdapter.java` | `RecyclerView.Adapter` for search result rows. |

### `ui/settings/` — App Settings

| File | Purpose |
|------|---------|
| `SettingsActivity.java` | Magnitude limit, night mode, layer toggles, time travel entry. |
| `SettingsViewModel.java` | Exposes settings state; persists to SharedPreferences. |

### `ui/starinfo/` — Object Detail View

| File | Purpose |
|------|---------|
| `StarInfoActivity.java` | Shows name, magnitude, RA/Dec, spectral type, Messier/NGC designation, educational blurb. Opens `ChatBottomSheetFragment` pre-seeded with the object context. |
| `StarInfoViewModel.java` | Loads star/DSO data from repositories. |

### `ui/highlights/` — Tonight's Sky

| File | Purpose |
|------|---------|
| `TonightsHighlightsFragment.java` | `BottomSheetDialogFragment` listing the most interesting visible objects tonight. Tapping navigates the sky canvas to that object. |

### `ui/timetravel/` — Time Travel

| File | Purpose |
|------|---------|
| `TimeTravelDialogFragment.java` | Date/time picker dialog. Updates `TimeTravelClock`, which propagates through `AstronomerModel` to all coordinate transforms. |

### `ui/education/` — Educational Content

| File | Purpose |
|------|---------|
| `EducationDetailActivity.java` | Rich detail view for constellations and planets, sourced from JSON assets. |

### `ui/widgets/`

| File | Purpose |
|------|---------|
| `CompassView.java` | Custom compass bearing view embedded in the sky map HUD. |

### `ui/common/` — Shared UI Components

| File | Purpose |
|------|---------|
| `ErrorDialog.java` | Standardised error alert dialog. |
| `LoadingDialog.java` | Progress dialog shown during async operations. |

---

### `native_/` — JNI Interfaces

The bridge between the Java/Kotlin app and the native astrometry C library.

| File | Purpose |
|------|---------|
| `AstrometryNative.java` | `static native` declarations: `detectStarsNative()`, `solveFieldNative()`, `computeDownsample()`, `bitmapToGrayscale()`. `System.loadLibrary("astrometry_native")` in static initializer. |
| `NativePlateSolver.java` | High-level plate solve API. `setDownsample(-1)` = auto (resolves based on image size), `setDownsample(≥1)` = explicit. `resolveDownsample()` only computes auto if the value is exactly -1. Copies index files from APK assets to internal storage before calling native code. |
| `StackingNative.java` | `static native` declarations for the stacking pipeline: `initStacking()`, `addFrame()`, `getStackedImage()`, `cancelStacking()`. |
| `ImageStackingManager.java` | Orchestrates multi-frame stacking. Calls `detectStarsNative` on each frame, feeds star lists to `StackingNative` for triangle match → RANSAC affine → bilinear warp → mean accumulate. Returns final stacked `Bitmap`. |
| `ConstellationOverlay.java` | Projects constellation line segments through a WCS solution onto the camera preview. |
| `WcsProjection.java` | Wraps the WCS matrix returned by `solveFieldNative` to project arbitrary RA/Dec to screen pixels. |

---

### `core/control/` — Sensors and Time

| File | Purpose |
|------|---------|
| `AstronomerModel.java` | Interface. `getPointing()` returns the current celestial direction the phone is aimed at. |
| `AstronomerModelImpl.java` | Consumes sensor rotation matrix and location, applies sidereal time offset, and produces a pointing vector in RA/Dec. |
| `SensorController.java` | Fuses accelerometer + magnetometer via `SensorManager.getRotationMatrix`. Delivers a 3×3 rotation matrix to `AstronomerModel`. |
| `LocationController.java` | GPS listener. Provides latitude/longitude used for local sidereal time and Alt/Az ↔ RA/Dec conversion. |
| `TimeTravelClock.java` | `Clock` implementation that returns either `System.currentTimeMillis()` or a user-chosen epoch. Switching the time causes `AstronomerModel` to recompute all planet positions. |
| `RealClock.java` | Production `Clock` implementation delegating to system time. |
| `MagneticDeclinationCalculator.java` / `RealMagneticDeclinationCalculator.java` | Computes magnetic declination at the current location using `GeomagneticField`. |

#### `core/control/space/` — Solar System Models (Kotlin)

| File | Purpose |
|------|---------|
| `Universe.kt` | `getRaDec(body, date)` — computes RA/Dec for any solar system body at an arbitrary epoch using Keplerian orbital elements. Kotlin `object` singleton. |
| `SolarSystemBody.kt` | Enum-like sealed class listing planets + Moon + Sun. |
| `OrbitalElements.kt` | Data class holding the six Keplerian elements per body. |
| `Moon.kt` | Specialised lunar position calculator (higher-order perturbation terms). |
| `Sun.kt` | Solar position from simplified VSOP87. |
| `SunOrbitingObject.kt` | Base class for planets orbiting the Sun. |
| `EarthOrbitingObject.kt` | Base class for the Moon. |
| `MovingObject.kt` | Interface for any body with a time-varying RA/Dec. |
| `CelestialObject.kt` | Data wrapper: RA, Dec, magnitude, label. |
| `SolarSystemObject.kt` | Associates a `SolarSystemBody` with display metadata. |

---

### `core/math/` — Astronomy Math (Kotlin)

| File | Purpose |
|------|---------|
| `Astronomy.kt` | Julian date computation, local sidereal time, equation of time. |
| `CoordinateManipulations.kt` | RA/Dec ↔ Alt/Az ↔ screen projection. Central to all rendering. |
| `Vector3.kt` | Immutable 3D vector with dot, cross, normalise operations. |
| `Matrix3x3.kt` | 3×3 rotation matrix. Used by `SensorController` and `AstronomerModel`. |
| `Matrix4x4.kt` | 4×4 homogeneous matrix for OpenGL path. |
| `RaDec.kt` | Typed value class for Right Ascension / Declination in radians. |
| `LatLong.kt` | Typed value class for geodetic latitude / longitude. |
| `MathUtils.kt` | Angular normalisation, degree/radian conversions, interpolation helpers. |
| `TimeUtils.kt` | Julian date from `Date`, modified Julian date, time formatting. |
| `Geometry.kt` | Angular separation, great-circle midpoint, screen-to-sphere unprojection. |

---

### `core/layers/` — Rendering Layers

Each layer is responsible for one category of sky objects. Layers are composited in order by `SkyCanvasView`.

| File | Purpose |
|------|---------|
| `Layer.java` | Interface. `draw(Canvas, Pointing)`. |
| `AbstractLayer.java` | Common projection and culling logic. |
| `StarsLayer.java` | Renders Hipparcos stars sized by magnitude. |
| `PlanetsLayer.java` | Queries `Universe.getRaDec` for each planet and renders with labels. |
| `ConstellationsLayer.java` | Renders constellation boundary outlines and stick-figure lines. |
| `GridLayer.java` | Celestial equator and hour-angle grid lines. |

---

### `core/renderer/` — Sky Rendering

| File | Purpose |
|------|---------|
| `SkyCanvasView.java` | Primary renderer. Canvas 2D (not OpenGL). Renders all layers in sequence. Pinch-to-zoom gesture (FOV clamped 20°–120°), manual pan mode with drag. Planet trajectory overlay (±30 days on long-press). Search navigation arrow. Reticle for object selection. `setFieldOfView()`, `setOrientation()`, `findNearestPlanet()`, `projectToScreen()`, `raDecToAltAz()`. |
| `SkyGLSurfaceView.java` | Alternate OpenGL ES 2.0 renderer surface (experimental path). |
| `SkyRenderer.java` | OpenGL render pass; manages `PointRenderer`, `LineRenderer`, `LabelRenderer`. |
| `ShaderProgram.java` | Compiles and links vertex/fragment shader pairs. |
| `PointRenderer.java` | Batched GL_POINTS draw call for star fields. |
| `LineRenderer.java` | Batched GL_LINES draw call for constellation lines and grid. |
| `LabelRenderer.java` | Billboarded text sprites for planet and star names. |

---

### `core/highlights/` — Tonight's Sky

| File | Purpose |
|------|---------|
| `TonightsHighlights.java` | Computes altitude of planets and bright stars for the current time and location, ranks them by altitude and brightness, returns the top visible objects. |

---

### `core/util/`

| File | Purpose |
|------|---------|
| `TimeConstants.java` | Seconds-per-day, Julian epoch constants. |
| `VisibleForTesting.java` | Annotation for package-private methods exposed for unit tests. |

---

### `data/` — Data Access

#### `data/api/`

| File | Purpose |
|------|---------|
| `OpenAIClient.java` | HTTP client for the Chat Completions API. Sends system prompt (with sky object context) and user messages. Streams Server-Sent Events tokens back to `ChatViewModel`. |

#### `data/model/` — Data Transfer Objects

| File | Purpose |
|------|---------|
| `StarData.java` | Hipparcos ID, RA, Dec, visual magnitude, spectral type, proper name. |
| `ConstellationData.java` | IAU abbreviation, full name, line segments (list of RA/Dec pairs). |
| `MessierObjectData.java` | M-number, NGC number, object type, RA, Dec, magnitude, size. |
| `GeocentricCoords.java` | X/Y/Z in the geocentric equatorial frame. |
| `CelestialObject.java` | Unified wrapper for any sky object (star, planet, DSO) with display metadata. |
| `ChatMessage.java` | Role (user/assistant) + content string. |
| `SkyBrightnessResult.java` | Bortle class (1–9), luminance value, descriptive label. |
| `LabelPrimitive.java` | Screen-space text label with anchor and style. |
| `LinePrimitive.java` | Pair of RA/Dec endpoints for constellation line rendering. |
| `PointPrimitive.java` | RA/Dec + magnitude for a single rendered point. |
| `Shape.java` | Enum for rendering shape types. |

#### `data/repository/`

| File | Purpose |
|------|---------|
| `StarRepository.java` | Interface. `getStars()` returns `List<StarData>`. |
| `StarRepositoryImpl.java` | Loads `stars.binary` via `ProtobufParser`. Caches result in memory. |
| `ConstellationRepository.java` | Interface. `getConstellations()`. |
| `ConstellationRepositoryImpl.java` | Loads `constellations.binary`. |
| `MessierRepository.java` | Interface. `getMessierObjects()`. |
| `MessierRepositoryImpl.java` | Loads `messier.binary`. |
| `EducationRepository.java` | Loads and parses JSON education assets. Provides content for `EducationDetailActivity` and the AstroBot system prompt. |

#### `data/education/`

| File | Purpose |
|------|---------|
| `ConstellationEducation.java` | POJO for a constellation's educational entry. |
| `PlanetEducation.java` | POJO for a planet's educational entry. |
| `SolarSystemEducation.java` | Container for all planet education records. |
| `StarEducationHelper.java` | Looks up a star's educational blurb from `full_education_content_100.json`. |

#### `data/parser/`

| File | Purpose |
|------|---------|
| `ProtobufParser.java` | Reads the custom binary format used by `stars.binary`, `constellations.binary`, and `messier.binary`. Converts raw bytes to typed model objects. |
| `AssetDataSource.java` | Opens asset files via `AssetManager`. Used by repositories and the parser. |

---

### `search/` — Search Engine

| File | Purpose |
|------|---------|
| `SearchIndex.java` | Builds and queries the search index across stars, constellations, and Messier objects. |
| `SearchResult.java` | Typed result: object reference + match type + relevance score. |
| `PrefixStore.java` | Prefix trie for fast autocomplete. |
| `SearchArrowView.java` | Animated arrow overlay drawn in `SkyCanvasView` pointing toward the selected search result. |

---

### `common/` — Shared Utilities

| File | Purpose |
|------|---------|
| `SkyDataProvider.java` | Interface aggregating all repositories for components that need cross-catalog access. |
| `LoadingState.java` | Sealed class: `Idle`, `Loading`, `Success<T>`, `Error`. |
| `Result.java` | Generic `Result<T>` with success/failure variants. |
| `ErrorHandler.java` | Maps exceptions to user-facing error messages. |
| `model/Pointing.java` | Phone's celestial pointing (RA, Dec, roll). |
| `model/ConstellationResult.java` | Resolved constellation name + boundaries. |
| `model/StarData.java` | Shared StarData alias (separate from `data/model/StarData.java` in legacy code). |

---

### `di/` — Dependency Injection (Dagger 2)

| File | Purpose |
|------|---------|
| `AppComponent.java` | Dagger component interface. Inject targets: `SkyMapActivity`, `StarInfoActivity`, `SearchActivity`, `SettingsActivity`, `ChatBottomSheetFragment`, `TonightsHighlightsFragment`. |
| `AppModule.java` | `@Module` providing all application-scoped singletons. |

**Singletons provided by `AppModule`:**

| Dependency | Implementation |
|-----------|----------------|
| `StarRepository` | `StarRepositoryImpl` (protobuf binary) |
| `ConstellationRepository` | `ConstellationRepositoryImpl` (protobuf binary) |
| `MessierRepository` | `MessierRepositoryImpl` (protobuf binary) |
| `EducationRepository` | JSON asset reader |
| `TimeTravelClock` | Real or user-set time |
| `SensorController` | Accelerometer + magnetometer fusion |
| `LocationController` | GPS lat/long |
| `AstronomerModelImpl` | Sensor + time → pointing vector |
| `Universe` | Kotlin object (solar system positions) |
| `OpenAIClient` | ChatGPT API (HTTP + SSE) |

---

## Native Layer (JNI / C)

The native library `libastrometry_native.so` is compiled from astrometry.net sources and two custom JNI bridge files.

### Build Configuration

- **Target library:** `libastrometry_native.so`
- **ABIs:** `arm64-v8a`, `armeabi-v7a`
- **NDK version:** 25.1
- **CMake version:** 3.22.1
- **Compiler flags:** `-O3 -fPIC -Wno-error`
- **System libraries linked:** `libm`, `liblog`, `libandroid`
- **Source count:** approximately 200 C files across 5 subdirectories

### JNI Bridge Files

#### `astrometry_jni.c` — Star Detection and Plate Solving

`detectStarsNative(byte[] grayscalePixels, int width, int height, float plim, float dpsf, int downsample)`:

1. Converts u8 pixel buffer to `float*` (matches solve-field's TFLOAT internal path).
2. Runs `image2xy_run` (simplexy algorithm: median-filter background estimation, DoG blob detection, peak fitting).
3. Applies resort: interleaves flux-sorted and raw-signal-sorted permutations (replicates `resort-xylist.c`).
4. Applies uniformize: divides image into a 10×10 spatial grid, bins stars, round-robin interleaves bins (replicates `uniformize.py`).
5. Applies edge-margin filter (removes stars within 1 pixel of image boundary).
6. Applies hot-pixel filter (removes isolated single-pixel peaks).
7. Returns float array `[x0, y0, flux0, bg0, x1, y1, flux1, bg1, ...]`.

`solveFieldNative(float[] stars, int nStars, int width, int height, String[] indexPaths, float scaleLow, float scaleHigh, float logOddsThreshold)`:

1. Builds `starxy_t` from the pre-sorted star array.
2. Loads each index file via `index_load`.
3. Configures solver: `verify_pix=1.0`, `distractor_ratio=0.25`, `codetol=0.01`, `parity=PARITY_BOTH`, `tweak_aborder=2`, `tweak_abporder=2`, `logratio_tokeep=logOddsThreshold`, `logratio_totune=log(1e6)`.
4. Iterates depth 10, 20, 30, … 200, calling `solver_run` at each depth.
5. On success, returns `[ra, dec, wcs_a, wcs_b, wcs_c, wcs_d, wcs_e, wcs_f]`.

**Adaptive plim retry logic** (in `ImageStackingActivity` / `NativePlateSolver`):
- Initial plim from Java call (default 8.0).
- If fewer than 30 stars returned, retry with lower plim values: 6.0 → 4.0.
- Retry only fires if the new plim is strictly less than the previously tried value (non-increasing guarantee prevents infinite loops).

#### `stacking_jni.c` — Image Stacking

Implements the full stacking pipeline in C for performance:

1. **Triangle asterism matching:** Forms triangles from the 5 nearest neighbours of each of the top 50 brightest stars. Computes scale-invariant side-length ratios `(s1/s0, s2/s0)` for sorted sides. Matches ratio pairs between reference and new frame using a libkd k-d tree search (radius 0.05).
2. **RANSAC affine estimation:** 100 iterations. Each iteration picks 3 random correspondences and solves the 6-parameter affine system via gsl-an LU decomposition. Counts inliers (reprojection error < 3 px). Keeps the best-fit matrix.
3. **Bilinear warp:** Applies the affine transform to remap each pixel of the new frame into the reference frame's coordinate system.
4. **Mean accumulator:** Maintains running float sums and a frame count. Divides on retrieval to produce the mean-stacked image.

### astrometry.net C Source Subdirectories

| Directory | Contents |
|-----------|----------|
| `astrometry/util/` | `simplexy.c`, `image2xy.c` (modified), `resort-xylist.c`, `fit-wcs.c`, `xylist.c`, `starxy.c`, `ctmf.c`, `dallpeaks.c`, `dfind.c`, `dsigma.c`, and ~40 support files |
| `astrometry/solver/` | `solver.c`, `verify.c` (reverted to upstream), `tweak2.c`, `blind.c`, `index.c`, `codekd.c`, `starkd.c`, and ~20 support files |
| `astrometry/libkd/` | K-D tree implementation: `kdtree.c`, `kdtree_fits_io.c`, `kdint_*.c` and headers |
| `astrometry/qfits-an/` | FITS I/O: `qfits_table.c`, `qfits_header.c`, `qfits_byteswap.c`, and ~10 files |
| `astrometry/gsl-an/` | GNU Scientific Library subset: BLAS, CBLAS, block, matrix, permutation, linalg (LU solve) |
| `astrometry/include/` | All header files |

### Modified Upstream Files

| File | Modification | Reason |
|------|-------------|--------|
| `astrometry/util/image2xy.c` line 68 | Added `s->image_u8 = NULL;` after `upconvert()` | Fixes crash: `simplexy_run` asserts that both `image` and `image_u8` are not simultaneously non-NULL. Without this fix, u8 images passed through the downsample path trigger an assertion failure. Bug exists in upstream astrometry.net. **Keep this fix.** |
| `astrometry/solver/verify.c` | Reverted to upstream | An earlier RoR (radius of relevance) modification was found to be unnecessary. The unmodified verify.c works correctly for all tested images. |

---

## Data Flow Diagrams

### Sky Rendering

```
Phone IMU (accel + mag)
    ↓ SensorController.getRotationMatrix()
3×3 rotation matrix
    ↓ AstronomerModelImpl
Pointing (RA, Dec) = celestial direction phone faces
    ↓ SkyCanvasView.setOrientation()
Per-frame Canvas draw:
    StarsLayer → PlanetsLayer → ConstellationsLayer → GridLayer
    ↓ CoordinateManipulations.projectToScreen()
Screen pixel (x, y) for each object
```

### Star Detection Pipeline

```
Camera Bitmap (JPEG from CameraX)
    ↓ AstrometryNative.bitmapToGrayscale()       [Java, ~20ms]
byte[] grayscale (width × height)
    ↓ AstrometryNative.computeDownsample()        [Java, -1 = auto]
Effective downsample factor
    ↓ detectStarsNative(pixels, w, h, plim, dpsf, ds)  [C, ~200-500ms]
    │   float[] → u8→float upconvert
    │   image2xy_run (simplexy)
    │   resort (flux/rawsignal interleave)
    │   uniformize (10×10 grid, round-robin)
    │   edge-margin filter
    │   hot-pixel filter
float[] [x, y, flux, bg, ...]
    ↓ if nStars < 30: retry with lower plim (6.0 → 4.0)
Star list ready for plate solving
```

### Plate Solving Pipeline

```
float[] star list (pre-sorted by resort+uniformize)
    ↓ NativePlateSolver.solve()
    │   copy index files from assets → internal storage (once)
    ↓ solveFieldNative(stars, nStars, w, h, indexPaths, scaleLow, scaleHigh, logOdds)  [C, seconds]
    │   build starxy_t
    │   load index files (4115–4119 FITS)
    │   depth iteration: 10, 20, 30, … 200
    │   solver_run() → quad match → verify → tweak2 WCS fit
float[] [ra, dec, wcs_a..f]  (or null if unsolved)
    ↓ PlateSolveResult
RA/Dec displayed; WCS matrix available for ConstellationOverlay projection
```

### Image Stacking Pipeline

```
N camera frames (Bitmaps)
    ↓ per frame: bitmapToGrayscale() + detectStarsNative()   [~200-500ms each]
Star lists for each frame
    ↓ StackingNative.addFrame(pixels, stars, refStars)
    │   triangle asterism match (libkd k-d tree)
    │   RANSAC affine (gsl-an LU solve, 100 iterations, 3px threshold)
    │   bilinear warp to reference frame
    │   float accumulator += warped pixels
After N frames: StackingNative.getStackedImage()
    │   divide accumulator by frame count
    ↓ Bitmap (grayscale, float→u8)
Stacked result displayed; optionally plate-solved
```

### AstroBot Chat

```
User taps Chat FAB in SkyMapActivity
    ↓ ChatBottomSheetFragment opens (selected sky object in Bundle)
User types query
    ↓ ChatViewModel.sendMessage(query)
    │   system prompt = app instructions + object context (name, RA, Dec, type)
    ↓ OpenAIClient.streamChatCompletion(messages)
    │   HTTP POST to api.openai.com/v1/chat/completions (stream=true)
    │   parse SSE tokens
    ↓ LiveData<List<ChatMessage>> → ChatMessageAdapter
Streamed response displayed token-by-token
```

---

## Coordinate System Transformations

The app moves data through five coordinate systems:

```
1. Device frame (phone x/y/z, sensor raw)
        ↓ SensorManager.getRotationMatrix()
2. World frame (North/East/Up at observer)
        ↓ AstronomerModelImpl + sidereal time
3. Equatorial frame (RA/Dec, fixed to stars)
      ↔ (via latitude, longitude, LST)
4. Horizontal frame (Altitude/Azimuth, local sky)
        ↓ CoordinateManipulations.projectToScreen()
5. Screen frame (px, py, clipped to display)
```

All transforms use `Matrix3x3` (pure Kotlin, no Android framework dependency) making them unit-testable without a device.

---

## Dependency Injection Architecture

Dagger 2 is used with a single component and single module, providing application-scoped singletons to all injection targets.

```
AstroApplication
    └── AppComponent (Dagger-generated)
            └── AppModule
                  ├── StarRepository           → StarRepositoryImpl
                  ├── ConstellationRepository  → ConstellationRepositoryImpl
                  ├── MessierRepository        → MessierRepositoryImpl
                  ├── EducationRepository      → JSON reader
                  ├── TimeTravelClock          → real or frozen time
                  ├── SensorController         → accel+mag fusion
                  ├── LocationController       → GPS
                  ├── AstronomerModelImpl      → pointing vector
                  ├── Universe (Kotlin object) → planet positions
                  └── OpenAIClient             → ChatGPT SSE
```

Activities and Fragments call `((AstroApplication) getApplication()).getAppComponent().inject(this)` in `onCreate`.

---

## Key Architectural Decisions

### Canvas 2D over OpenGL for Primary Renderer
`SkyCanvasView` uses the Android `Canvas` API rather than OpenGL ES. This works on all devices and emulators without EGL setup. The OpenGL path (`SkyGLSurfaceView`) exists as an experimental alternative.

### astrometry.net Compiled from Source
The ~200 C files are compiled into `libastrometry_native.so` via the NDK rather than linking against the system `libastrometry.so`. This allows the `image2xy.c` assertion bug fix to be applied, ensures ABI compatibility, and eliminates runtime dependency on an installed binary.

### Triangle Asterism Matching over FFT Correlation
`android_live_stacker` (the reference project) uses FFT phase correlation which handles only translation. A handheld phone rotates between exposures. Triangle matching uses scale-invariant side-length ratios, making it invariant to rotation, scale, and translation simultaneously.

### Strictly Non-Increasing plim Retry
The adaptive retry in `ImageStackingActivity` guarantees that `plim` values are tried in strictly decreasing order (8.0 → 6.0 → 4.0) with no repeat. This prevents any possibility of an infinite retry loop and ensures each retry is genuinely more permissive than the last.

### Tooltip rootView-Relative Coordinates
`TooltipManager` requires a `rootView` parameter when showing tooltips inside dialogs. Without this, anchor coordinates are in window space and the punch-through circle is drawn in the wrong position because dialog windows have their own coordinate origin. The manager corrects for this by computing the offset between the window origin and the rootView origin.

### Resort + Uniformize Star Ordering
solve-field applies two reordering steps before the quad solver sees stars. Without replication of these steps, the first ~20 detected stars cluster in the brightest image region (the Orion Nebula core), and the solver cannot form field-spanning quads. After resort + uniformize, the first 9 stars span the full field, and the solver succeeds at depth 21–30 rather than failing entirely.
