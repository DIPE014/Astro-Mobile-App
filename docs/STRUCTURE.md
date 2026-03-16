# Project Structure

## Root Directory

```
Astro-Mobile-App/
├── app/                    # Android application module
├── docs/                   # Documentation
│   ├── README.md           # Documentation index
│   ├── ARCHITECTURE.md     # System design and data flows
│   ├── STRUCTURE.md        # This file — directory reference
│   ├── CORE_CONCEPTS.md    # Astronomy and algorithm explanations
│   └── SETUP.md            # Development environment setup
├── gradle/                 # Gradle wrapper (gradlew, gradlew.bat)
├── build.gradle            # Root build config
├── settings.gradle         # Module settings (includes :app)
├── CHANGELOG.md            # Weekly release notes
├── README.md               # Project overview (user-facing)
└── CLAUDE.md               # AI assistant context (astrometry JNI details)
```

---

## App Module (`app/`)

```
app/
├── build.gradle            # NDK 25.1, CMake 3.22.1, minSdk 26, targetSdk 34
│                           # abiFilters: arm64-v8a, armeabi-v7a
│                           # Dagger 2 annotation processor
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── cpp/
    │   ├── java/
    │   ├── res/
    │   └── assets/
    └── test/
```

### `AndroidManifest.xml`

Declared permissions:
- `CAMERA` — live sky view AR mode and image capture
- `ACCESS_FINE_LOCATION` — GPS for local sidereal time and Alt/Az transforms
- `INTERNET` — OpenAI chat API

Declared activities: `MainActivity`, `IntroSplashActivity`, `SkyMapActivity`, `SearchActivity`, `StarInfoActivity`, `SettingsActivity`, `SkyBrightnessActivity`, `PlateSolveActivity`, `ImageStackingActivity`, `EducationDetailActivity`, `OnboardingActivity`.

---

## Native Sources (`app/src/main/cpp/`)

```
cpp/
├── CMakeLists.txt              # Native build definition
│                               #   target: libastrometry_native.so
│                               #   ABIs: arm64-v8a, armeabi-v7a
│                               #   flags: -O3 -fPIC -Wno-error
│                               #   links: libm, liblog, libandroid
├── Makefile.test               # WSL host build for local testing
│
├── jni/
│   ├── astrometry_jni.c        # Star detection + plate solving JNI bridge
│   │                           #   detectStarsNative(): u8→float, simplexy,
│   │                           #     resort, uniformize, edge filter, hot-pixel filter
│   │                           #   solveFieldNative(): quad match, verify, WCS fit
│   └── stacking_jni.c          # Image stacking JNI bridge
│                               #   triangle asterism match (libkd)
│                               #   RANSAC affine (gsl-an LU solve, 100 iters)
│                               #   bilinear warp, float mean accumulator
│
└── astrometry/                 # astrometry.net C sources (~200 files)
    ├── include/                # All header files (.h)
    │
    ├── util/                   # Star detection utilities
    │   ├── simplexy.c          # Core blob detection algorithm
    │   ├── image2xy.c          # *** MODIFIED *** (see below)
    │   ├── resort-xylist.c     # Flux/rawsignal interleave sort
    │   ├── fit-wcs.c           # WCS polynomial fitting
    │   ├── xylist.c            # Star list I/O
    │   ├── starxy.c            # Star coordinate array operations
    │   ├── ctmf.c              # Constant-time median filter (background)
    │   ├── dallpeaks.c         # Peak detection in filtered image
    │   ├── dfind.c             # Connected component finder
    │   ├── dsigma.c            # Robust sigma estimation
    │   └── (and ~35 other support files)
    │
    ├── solver/                 # Plate solver
    │   ├── solver.c            # Main quad-matching loop
    │   ├── verify.c            # Hypothesis verification (reverted to upstream)
    │   ├── tweak2.c            # SIP polynomial WCS refinement
    │   ├── blind.c             # Top-level solve orchestration
    │   ├── index.c             # Index file loading and management
    │   ├── codekd.c            # Code k-D tree (quad code lookup)
    │   ├── starkd.c            # Star k-D tree (index star positions)
    │   └── (and ~15 other support files)
    │
    ├── libkd/                  # K-D tree library
    │   ├── kdtree.c            # Core k-D tree build/query
    │   ├── kdtree_fits_io.c    # FITS serialisation for index files
    │   └── kdint_*.c           # Type-specialised integer implementations
    │
    ├── qfits-an/               # FITS I/O library (astrometry.net fork)
    │   ├── qfits_table.c       # FITS binary table read/write
    │   ├── qfits_header.c      # FITS header parsing
    │   ├── qfits_byteswap.c    # Endian conversion
    │   └── (and ~7 other files)
    │
    └── gsl-an/                 # GNU Scientific Library subset
        ├── blas/               # BLAS level-1/2/3 routines
        ├── cblas/              # C BLAS interface
        ├── block/              # Memory block allocator
        ├── matrix/             # Dense matrix operations
        ├── permutation/        # Permutation vectors (for LU)
        └── linalg/             # LU decomposition (used in RANSAC affine solve)
```

### Modified Upstream Files

| File | Change | Reason |
|------|--------|--------|
| `astrometry/util/image2xy.c` line 68 | Added `s->image_u8 = NULL;` after `upconvert()` call | `simplexy_run` asserts that `image` and `image_u8` are not simultaneously set. Without this line, any u8 input that goes through the downsample path crashes with an assertion failure. Bug is present in upstream astrometry.net. This fix must be kept. |
| `astrometry/solver/verify.c` | Reverted to upstream | An earlier modification to the radius-of-relevance (RoR) check was found to be unnecessary. The unmodified upstream code works correctly for all tested images. |

---

## Java/Kotlin Sources (`app/src/main/java/com/astro/app/`)

```
com/astro/app/
│
├── AstroApplication.java           # Application subclass; initialises Dagger AppComponent
├── MainActivity.java               # Launch trampoline → IntroSplashActivity or SkyMapActivity
├── IntroSplashActivity.java        # Hosts StarFieldView warp animation (root package, legacy location)
│
├── ui/                             # Presentation Layer
│   │
│   ├── skymap/                     # Main AR sky view
│   │   ├── SkyMapActivity.java         # Camera + sensors + sky canvas. Draggable FAB
│   │   │                               # radial menu (MotionLayout). Night mode toggle.
│   │   │                               # AR toggle. Time travel. Circular reveal transition.
│   │   ├── SkyMapViewModel.java        # Night mode, layer visibility, magnitude limit state
│   │   ├── AROverlayManager.java       # Calibrates sky canvas FOV to physical camera FOV
│   │   ├── CameraManager.java          # CameraX lifecycle (Preview + ImageCapture use-cases)
│   │   ├── CameraPermissionHandler.java # ActivityResultLauncher for CAMERA permission
│   │   ├── ConstellationBoundaryResolver.java  # IAU boundary JSON → constellation name
│   │   └── SkyOverlayView.java         # Transparent overlay for reticle and chip strip
│   │
│   ├── onboarding/                 # Tutorial system
│   │   ├── OnboardingActivity.java     # 6-page ViewPager2 intro walkthrough (first launch)
│   │   ├── OnboardingPagerAdapter.java # Supplies pages to ViewPager2
│   │   ├── TooltipManager.java         # Per-screen tooltip sequence controller.
│   │   │                               # SharedPreferences completion state per screen.
│   │   │                               # rootView-relative coordinate correction.
│   │   │                               # Keys: KEY_SKYMAP_TUTORIAL, KEY_CHAT_TUTORIAL,
│   │   │                               #   KEY_DETECT_TUTORIAL, KEY_SETTINGS_TUTORIAL,
│   │   │                               #   KEY_SEARCH_TUTORIAL
│   │   ├── TooltipView.java            # Full-screen overlay with punch-through highlight circle.
│   │   │                               # Step counter, Skip / Next / Got it buttons.
│   │   │                               # Interactive tap-through to anchor views.
│   │   └── TooltipConfig.java          # Builder pattern config. Positions: ABOVE/BELOW/LEFT/
│   │                                   # RIGHT/CENTER with auto-fallback. Interactive mode,
│   │                                   # extra highlight views, onShowAction callback.
│   │
│   ├── intro/                      # Animated splash screen
│   │   └── StarFieldView.java          # 220-star field + parallax + meteor + warp tunnel.
│   │                                   # Fires warpCompleteListener; delivery deferred if detached.
│   │
│   ├── stacking/                   # Detect and Stack
│   │   └── ImageStackingActivity.java  # Unified UI for single-frame plate solving and
│   │                                   # multi-frame stacking. CameraX TakePicture contract.
│   │                                   # Gallery picker. MAX_PROCESSING_DIMENSION=4096.
│   │                                   # Adaptive plim retry (8.0→6.0→4.0, strictly decreasing).
│   │                                   # Photography tips + example capture dialogs.
│   │
│   ├── platesolve/                 # Legacy plate solve UI
│   │   └── PlateSolveActivity.java
│   │
│   ├── chat/                       # AstroBot AI assistant
│   │   ├── ChatBottomSheetFragment.java  # BottomSheetDialogFragment. Sky object context in
│   │   │                                 # system prompt. 300ms tooltip delay with
│   │   │                                 # removeCallbacks cleanup in onDestroyView.
│   │   ├── ChatViewModel.java            # Message list LiveData; streams SSE tokens
│   │   └── ChatMessageAdapter.java       # RecyclerView.Adapter (user/bot view types)
│   │
│   ├── search/                     # Object search
│   │   ├── SearchActivity.java         # Text search; drives SearchArrowView
│   │   └── SearchResultAdapter.java    # RecyclerView.Adapter for results
│   │
│   ├── settings/                   # App settings
│   │   ├── SettingsActivity.java
│   │   └── SettingsViewModel.java      # Persists to SharedPreferences
│   │
│   ├── starinfo/                   # Sky object detail
│   │   ├── StarInfoActivity.java       # Name, magnitude, RA/Dec, spectral type, education.
│   │   │                               # Opens ChatBottomSheetFragment pre-seeded with context.
│   │   └── StarInfoViewModel.java
│   │
│   ├── highlights/                 # Tonight's sky bottom sheet
│   │   └── TonightsHighlightsFragment.java  # Lists top visible objects; tap navigates canvas
│   │
│   ├── skybrightness/              # Light pollution meter
│   │   ├── SkyBrightnessActivity.java
│   │   ├── SkyBrightnessAnalyzer.java  # ImageProxy → Bortle scale 1-9
│   │   └── BortleScaleView.java        # Custom arc gauge view
│   │
│   ├── education/                  # Educational content
│   │   └── EducationDetailActivity.java  # Rich constellation/planet detail from JSON assets
│   │
│   ├── timetravel/                 # Time travel dialog
│   │   └── TimeTravelDialogFragment.java  # Date/time picker → updates TimeTravelClock
│   │
│   ├── widgets/                    # Standalone custom views
│   │   └── CompassView.java            # Compass bearing widget in sky map HUD
│   │
│   └── common/                     # Shared UI components
│       ├── ErrorDialog.java
│       └── LoadingDialog.java
│
├── native_/                        # JNI Interfaces
│   ├── AstrometryNative.java           # static native: detectStarsNative, solveFieldNative,
│   │                                   # computeDownsample, bitmapToGrayscale.
│   │                                   # System.loadLibrary("astrometry_native")
│   ├── NativePlateSolver.java          # High-level plate solve API.
│   │                                   # setDownsample(-1=auto, ≥1=explicit).
│   │                                   # Copies index FITS files from assets to internal storage.
│   ├── StackingNative.java             # static native: initStacking, addFrame,
│   │                                   # getStackedImage, cancelStacking
│   ├── ImageStackingManager.java       # Multi-frame pipeline: detectStars per frame →
│   │                                   # triangle match → RANSAC → warp → accumulate
│   ├── ConstellationOverlay.java       # Projects constellation lines through WCS onto camera
│   └── WcsProjection.java             # Wraps WCS matrix for RA/Dec → screen projection
│
├── core/                           # Domain / Business Logic Layer
│   │
│   ├── control/                    # Sensor and time controllers
│   │   ├── AstronomerModel.java        # Interface: getPointing() → RA/Dec
│   │   ├── AstronomerModelImpl.java    # Sensor rotation matrix + location + time → pointing
│   │   ├── Clock.java                  # Interface: getTimeInMillisSinceEpoch()
│   │   ├── RealClock.java              # Production: System.currentTimeMillis()
│   │   ├── TimeTravelClock.java        # Real or user-frozen time
│   │   ├── LocationController.java     # GPS listener → lat/long
│   │   ├── SensorController.java       # Accel + mag → 3×3 rotation matrix
│   │   ├── MagneticDeclinationCalculator.java       # Interface
│   │   ├── RealMagneticDeclinationCalculator.java   # GeomagneticField implementation
│   │   ├── OrbitalElements.kt          # Six Keplerian elements per solar system body
│   │   ├── SolarSystemBody.kt          # Enum: planets, Moon, Sun
│   │   └── space/                      # Solar system models (Kotlin)
│   │       ├── Universe.kt             # getRaDec(body, date) — Kotlin object singleton
│   │       ├── CelestialObject.kt      # RA, Dec, magnitude, label wrapper
│   │       ├── SolarSystemObject.kt    # Associates SolarSystemBody with display metadata
│   │       ├── Moon.kt                 # Higher-order perturbation lunar model
│   │       ├── Sun.kt                  # Simplified VSOP87 solar position
│   │       ├── SunOrbitingObject.kt    # Base: planets orbiting the Sun
│   │       ├── EarthOrbitingObject.kt  # Base: Moon
│   │       └── MovingObject.kt         # Interface: time-varying RA/Dec
│   │
│   ├── math/ (Kotlin)              # Astronomy math utilities
│   │   ├── Astronomy.kt                # Julian date, local sidereal time, equation of time
│   │   ├── CoordinateManipulations.kt  # RA/Dec ↔ Alt/Az ↔ screen projection
│   │   ├── Vector3.kt                  # Immutable 3D vector (dot, cross, normalise)
│   │   ├── Matrix3x3.kt                # 3×3 rotation matrix
│   │   ├── Matrix4x4.kt                # 4×4 homogeneous matrix (OpenGL path)
│   │   ├── RaDec.kt                    # Typed RA/Dec value class (radians)
│   │   ├── LatLong.kt                  # Typed lat/long value class
│   │   ├── MathUtils.kt                # Angle normalisation, deg/rad, interpolation
│   │   ├── TimeUtils.kt                # Julian date from Date, modified JD, formatting
│   │   └── Geometry.kt                 # Angular separation, great-circle midpoint
│   │
│   ├── layers/                     # Rendering layers (composited by SkyCanvasView)
│   │   ├── Layer.java                  # Interface: draw(Canvas, Pointing)
│   │   ├── AbstractLayer.java          # Common projection and culling
│   │   ├── StarsLayer.java             # Hipparcos stars, sized by magnitude
│   │   ├── PlanetsLayer.java           # Universe.getRaDec per planet + labels
│   │   ├── ConstellationsLayer.java    # Boundary outlines and stick-figure lines
│   │   └── GridLayer.java              # Celestial equator and hour-angle grid
│   │
│   ├── renderer/                   # Sky rendering
│   │   ├── SkyCanvasView.java          # Primary Canvas 2D renderer. All layer composition.
│   │   │                               # Pinch-to-zoom (20°–120° FOV). Manual pan mode.
│   │   │                               # Planet trajectory overlay (±30 days, long-press).
│   │   │                               # Search navigation arrow. Reticle + object selection.
│   │   │                               # setFieldOfView(), setOrientation(), findNearestPlanet(),
│   │   │                               # projectToScreen(), raDecToAltAz()
│   │   ├── SkyGLSurfaceView.java       # Alternate OpenGL ES 2.0 surface (experimental)
│   │   ├── SkyRenderer.java            # OpenGL render pass; manages sub-renderers
│   │   ├── ShaderProgram.java          # Vertex/fragment shader compilation and linking
│   │   ├── PointRenderer.java          # Batched GL_POINTS for star fields
│   │   ├── LineRenderer.java           # Batched GL_LINES for constellation and grid
│   │   └── LabelRenderer.java          # Billboarded text sprites for names
│   │
│   ├── highlights/                 # Tonight's sky computation
│   │   └── TonightsHighlights.java     # Ranks visible planets and bright stars by altitude
│   │
│   └── util/
│       ├── TimeConstants.java          # Seconds/day, Julian epoch values
│       └── VisibleForTesting.java      # Annotation for test-exposed package-private methods
│
├── data/                           # Data Layer
│   │
│   ├── api/
│   │   └── OpenAIClient.java           # HTTP POST to Chat Completions API; SSE token streaming
│   │
│   ├── model/                      # Data transfer objects
│   │   ├── StarData.java               # Hipparcos ID, RA, Dec, magnitude, spectral type, name
│   │   ├── ConstellationData.java      # IAU abbreviation, full name, line segments
│   │   ├── MessierObjectData.java      # M-number, NGC, type, RA, Dec, magnitude, size
│   │   ├── GeocentricCoords.java       # X/Y/Z in geocentric equatorial frame
│   │   ├── CelestialObject.java        # Unified sky object wrapper (star/planet/DSO)
│   │   ├── ChatMessage.java            # Role (user/assistant) + content
│   │   ├── SkyBrightnessResult.java    # Bortle class, luminance, label
│   │   ├── LabelPrimitive.java         # Screen-space text label with anchor and style
│   │   ├── LinePrimitive.java          # RA/Dec endpoint pair for constellation lines
│   │   ├── PointPrimitive.java         # RA/Dec + magnitude for a rendered point
│   │   └── Shape.java                  # Enum of rendering shape types
│   │
│   ├── repository/                 # Data access (interface + implementation pairs)
│   │   ├── StarRepository.java         # Interface: getStars() → List<StarData>
│   │   ├── StarRepositoryImpl.java     # Loads stars.binary via ProtobufParser; memory cache
│   │   ├── ConstellationRepository.java
│   │   ├── ConstellationRepositoryImpl.java  # Loads constellations.binary
│   │   ├── MessierRepository.java
│   │   └── MessierRepositoryImpl.java  # Loads messier.binary
│   │
│   ├── education/                  # Education content access
│   │   ├── EducationRepository.java    # Loads and parses JSON education assets
│   │   ├── ConstellationEducation.java # POJO for constellation educational entry
│   │   ├── PlanetEducation.java        # POJO for planet educational entry
│   │   ├── SolarSystemEducation.java   # Container for all planet education records
│   │   └── StarEducationHelper.java    # Looks up star blurb from full_education_content_100.json
│   │
│   └── parser/
│       ├── ProtobufParser.java         # Reads custom binary format (stars/constellations/messier)
│       └── AssetDataSource.java        # Opens assets via AssetManager
│
├── search/                         # Search Engine
│   ├── SearchIndex.java                # Builds and queries index (stars, constellations, Messier)
│   ├── SearchResult.java               # Typed result with object reference and relevance score
│   ├── PrefixStore.java                # Prefix trie for autocomplete
│   └── SearchArrowView.java            # Animated navigation arrow pointing to selected object
│
├── common/                         # Shared Utilities
│   ├── SkyDataProvider.java            # Interface aggregating all repositories
│   ├── LoadingState.java               # Sealed: Idle / Loading / Success<T> / Error
│   ├── Result.java                     # Generic Result<T> (success/failure)
│   ├── ErrorHandler.java               # Exception → user-facing message
│   └── model/
│       ├── Pointing.java               # Phone's celestial pointing (RA, Dec, roll)
│       ├── ConstellationResult.java    # Resolved constellation name + boundaries
│       └── StarData.java               # Legacy shared StarData alias
│
└── di/                             # Dependency Injection (Dagger 2)
    ├── AppComponent.java               # @Component; inject targets: activities + fragments
    └── AppModule.java                  # @Module; all @Singleton providers
```

---

## Resources (`app/src/main/res/`)

```
res/
│
├── layout/                         # 24 XML layout files
│   ├── activity_sky_map.xml        # Main AR sky view (ConstraintLayout + MotionLayout FAB)
│   ├── activity_image_stacking.xml # Detect & Stack UI
│   ├── activity_plate_solve.xml    # Legacy plate solve UI
│   ├── activity_search.xml
│   ├── activity_star_info.xml
│   ├── activity_settings.xml
│   ├── activity_sky_brightness.xml
│   ├── activity_education_detail.xml
│   ├── activity_onboarding.xml
│   ├── activity_intro_splash.xml
│   ├── activity_main.xml
│   ├── fragment_chat_bottom_sheet.xml
│   ├── fragment_tonights_highlights.xml
│   ├── dialog_loading.xml
│   ├── dialog_time_travel.xml
│   ├── dialog_sky_brightness.xml
│   ├── dialog_plate_solve_tips.xml
│   ├── dialog_example_capture.xml
│   ├── item_chat_message_bot.xml
│   ├── item_chat_message_user.xml
│   ├── item_search_result.xml
│   ├── item_related_star.xml
│   ├── item_stacking_thumbnail.xml
│   └── item_onboarding_page.xml
│
├── xml/
│   ├── fab_menu_scene.xml          # MotionLayout constraint set for FAB expand/collapse
│   ├── fab_menu_visible.xml        # MotionLayout visible state constraints
│   └── file_paths.xml              # FileProvider paths (for sharing captured images)
│
├── values/
│   ├── strings.xml                 # All UI strings; 40+ tooltip step strings
│   ├── colors.xml                  # Primary palette (deep space dark + accent)
│   ├── colors_search_details.xml   # Search-specific colour overrides
│   ├── dimens.xml                  # FAB sizes, tooltip metrics, card radii
│   ├── themes.xml                  # Light theme (extends MaterialComponents)
│   ├── styles.xml                  # Button, chip, card styles
│   ├── attrs.xml                   # Custom view attributes (TooltipView, BortleScaleView)
│   └── ic_launcher_background.xml
│
├── values-night/
│   └── themes.xml                  # Night mode theme overrides
│
├── drawable/                       # 22 drawable resources
│   ├── bg_bottom_sheet_handle.xml  # Rounded handle pill for bottom sheets
│   ├── bg_chat_bubble_bot.xml      # Bot message bubble shape
│   ├── bg_chat_bubble_user.xml     # User message bubble shape
│   ├── bg_circle_primary.xml       # Circular FAB background
│   ├── bg_circle_surface.xml       # Circular surface-colour button
│   ├── bg_gps_indicator.xml        # GPS status dot
│   ├── bg_gradient.xml             # Sky gradient (intro screen)
│   ├── circle_primary_container.xml
│   ├── ic_approx_coord.xml         # Vector: approximate coordinates icon
│   ├── ic_asterism.xml             # Vector: asterism icon
│   ├── ic_background_origin.xml    # Vector: mythology origin icon
│   ├── ic_best_month.xml           # Vector: calendar icon
│   ├── ic_family.xml               # Vector: constellation family icon
│   ├── ic_formalized.xml           # Vector: IAU formalized date icon
│   ├── ic_launcher_foreground.xml  # App icon foreground layer
│   ├── ic_layers.xml               # Vector: layer toggle icon
│   ├── ic_major_stars.xml          # Vector: major stars icon
│   ├── ic_source.xml               # Vector: source/reference icon
│   ├── intro_star.gif              # Animated star GIF for splash screen
│   └── example_capture.jpg         # Sample astrophoto shown in capture tips dialog
│
├── anim/
│   ├── fade_in_pulse.xml           # Pulsing fade-in for UI elements
│   ├── intro_fade_in.xml           # Splash screen content fade in
│   ├── intro_fade_out.xml          # Splash screen fade out before transition
│   └── title_zoom_exit.xml         # Title zoom-out on transition
│
├── font/                           # Exo 2 typeface (18 weight variants)
│   ├── exo_regular.ttf, exo_medium.ttf, exo_bold.ttf, exo_black.ttf
│   ├── exo_light.ttf, exo_thin.ttf, exo_extralight.ttf, exo_extrabold.ttf
│   ├── exo_semibold.ttf
│   └── *italic.ttf variants (9 italic counterparts)
│
└── mipmap-anydpi-v26/
    ├── ic_launcher.xml             # Adaptive icon (foreground + background layers)
    └── ic_launcher_round.xml       # Round variant adaptive icon
```

---

## Assets (`app/src/main/assets/`)

```
assets/
│
├── stars.binary                    # Hipparcos star catalog (~60,000 stars)
│                                   # Binary protobuf: HIP ID, RA, Dec, Vmag, spectral type, name
│
├── constellations.binary           # 88 IAU constellations
│                                   # Binary protobuf: abbreviation, name, line segments (RA/Dec pairs)
│
├── messier.binary                  # 110 Messier deep-sky objects
│                                   # Binary protobuf: M-number, NGC, type, RA, Dec, Vmag, size
│
├── constellations.json             # Constellation metadata (abbreviation → full name, genitive,
│                                   # mythology, family, best month, quadrant)
│
├── constellation_lines.json        # Stick-figure line endpoints (supplementary to binary)
│
├── constellation_education_latest.json  # Latest constellation educational content
│
├── constellation_education_no_is_zodiac.json  # Educational content excl. zodiac flag
│
├── bound.json                      # IAU constellation boundary segments (full precision)
│
├── bound_18.json                   # Simplified boundaries (18 vertices max per constellation)
│
├── bound_edges_18.json             # Boundary edge list (for ConstellationBoundaryResolver)
│
├── bound_in_18.json                # Interior points (used for point-in-constellation test)
│
├── education_content.json          # Mixed star/planet/constellation educational records
│
├── education_content_with_radec.json  # Education records with RA/Dec for navigation
│
├── full_education_content_100.json # Top 100 brightest stars with detailed educational content
│                                   # (used by StarEducationHelper and StarInfoActivity)
│
├── planets_education.json          # Per-planet educational records (used by PlanetEducation)
│
└── indexes/                        # Plate solving FITS index files
    ├── index-4115.fits             # Astrometry.net index 4115 (scale ~11–16 arcmin quads)
    ├── index-4116.fits             # Astrometry.net index 4116 (scale ~16–22 arcmin quads)
    ├── index-4117.fits             # Astrometry.net index 4117 (scale ~22–30 arcmin quads)
    ├── index-4118.fits             # Astrometry.net index 4118 (scale ~30–42 arcmin quads)
    └── index-4119.fits             # Astrometry.net index 4119 (scale ~42–60 arcmin quads)
                                    # All 5 indexes cover the Orion region and nearby sky.
                                    # Copied from assets to internal storage by NativePlateSolver
                                    # on first use (index files cannot be mmap'd from APK).
```

---

## Tests (`app/src/test/`)

```
test/java/com/astro/app/
├── core/math/
│   ├── Vector3Test.java            # Dot product, cross product, normalisation edge cases
│   ├── Matrix3x3Test.java          # Matrix multiplication, rotation composition
│   ├── RaDecTest.java              # Angle normalisation, degree/radian conversion
│   └── TimeUtilsTest.java          # Julian date, modified Julian date computation
└── data/model/
    ├── StarDataTest.java           # StarData construction, magnitude comparison
    └── GeocentricCoordsTest.java   # Coordinate arithmetic
```

Total: 6 test files. All are JVM unit tests (no Android framework dependency). Run with `./gradlew test`.

---

## Key File Relationships

The diagram below traces how a plate solve request flows through the file system:

```
ImageStackingActivity.java
    └── calls NativePlateSolver.java
            └── calls AstrometryNative.java (JNI declarations)
                    ↓ JNI boundary
                    astrometry_jni.c
                        └── image2xy.c (MODIFIED — assertion fix)
                        └── resort-xylist.c, simplexy.c, ctmf.c, ...
                        └── solver.c, verify.c (upstream), tweak2.c, ...
                        └── libkd/kdtree.c (index star lookup)
                        └── qfits-an/ (FITS index file I/O)
                        └── gsl-an/linalg/ (WCS polynomial fitting)
            └── reads assets/indexes/index-4115..4119.fits
                (copied to getFilesDir() by NativePlateSolver on first use)
```

Stacking flow:

```
ImageStackingActivity.java
    └── calls ImageStackingManager.java
            └── calls AstrometryNative.java (detectStarsNative per frame)
            └── calls StackingNative.java (addFrame, getStackedImage)
                    ↓ JNI boundary
                    stacking_jni.c
                        └── libkd/kdtree.c (triangle match k-d tree query)
                        └── gsl-an/linalg/ (RANSAC affine LU solve)
```

Sky rendering flow:

```
SkyMapActivity.java
    └── SensorController.java → rotation matrix
    └── LocationController.java → lat/long
    └── AstronomerModelImpl.java → Pointing (RA, Dec)
    └── SkyCanvasView.java
            └── StarsLayer.java → StarRepositoryImpl.java → ProtobufParser.java → stars.binary
            └── PlanetsLayer.java → Universe.kt → OrbitalElements.kt
            └── ConstellationsLayer.java → ConstellationRepositoryImpl.java → constellations.binary
            └── CoordinateManipulations.kt (projection)
```

Tooltip flow:

```
SkyMapActivity.java (or any other screen)
    └── TooltipManager.java
            └── TooltipView.java (draws over the activity window)
            └── TooltipConfig.java (position, interactive mode, callbacks)
            └── SharedPreferences (per-screen completion keys)
```
