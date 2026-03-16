# Astro Mobile App

> A sophisticated Android astronomy app with real-time AR sky mapping, on-device plate solving, AI-powered chat, and image stacking — all running natively on your phone.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)
![Language](https://img.shields.io/badge/language-Java%20%2F%20Kotlin-orange)
![NDK](https://img.shields.io/badge/NDK-25.1-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-34-blue)
![Architecture](https://img.shields.io/badge/architecture-MVVM%20%2B%20Dagger%202-purple)
![License](https://img.shields.io/badge/license-Educational-lightgrey)

---

## Table of Contents

1. [Overview](#overview)
2. [Screenshots](#screenshots)
3. [Features](#features)
   - [Real-Time AR Sky Map](#1-real-time-ar-sky-map)
   - [Pinch-to-Zoom & Manual Pan](#2-pinch-to-zoom--manual-pan)
   - [Planet Trajectories](#3-planet-trajectories)
   - [Tonight's Highlights](#4-tonights-highlights)
   - [Time Travel](#5-time-travel)
   - [AstroBot AI Chat](#6-astrobot-ai-chat)
   - [Sky Brightness Meter](#7-sky-brightness-meter)
   - [Image Stacking](#8-image-stacking)
   - [Plate Solving](#9-plate-solving)
   - [Star & Object Information](#10-star--object-information)
   - [Search & Navigate](#11-search--navigate)
   - [Night Mode](#12-night-mode)
   - [3D Compass](#13-3d-compass)
   - [Onboarding & Tutorials](#14-onboarding--tutorials)
   - [Intro Splash Screen](#15-intro-splash-screen)
   - [Constellation Identification](#16-constellation-identification)
   - [Educational Content](#17-educational-content)
4. [Tech Stack](#tech-stack)
5. [Requirements](#requirements)
6. [Build Instructions](#build-instructions)
7. [How to Use](#how-to-use)
8. [Architecture](#architecture)
9. [Native Library (JNI)](#native-library-jni)
10. [Changelog](#changelog)
11. [Credits](#credits)
12. [License](#license)

---

## Overview

Astro Mobile App transforms your Android phone into a full-featured astronomy instrument. Hold it up to the sky and see 60,000+ stars, 88 constellations, and 8 planets rendered in augmented reality, aligned to your exact GPS location and device orientation in real time.

Beyond sky mapping, the app includes an on-device plate solver (astrometry.net compiled via NDK), an image stacking pipeline for deep-sky photography, a GPT-powered astronomy assistant, and a comprehensive interactive onboarding system — all without requiring an external server for the core astronomy functions.

---

## Screenshots

*Screenshots coming soon. The app requires a physical device with camera and motion sensors.*

---

## Features

### 1. Real-Time AR Sky Map

The sky map is the heart of the app. It renders the entire visible sky using the device's accelerometer, magnetometer, and gyroscope to determine where the camera is pointing.

- **60,000+ Hipparcos stars** rendered at accurate positions and magnitudes, with brightness and size scaled to visual magnitude
- **88 constellation line figures** with labels at each constellation's centroid
- **Constellation boundaries** (IAU official, 1930 epoch) rendered as boundary lines and used for tap-to-identify
- **110 Messier deep sky objects** — galaxies (diamonds), open and globular clusters (squares), nebulae (glowing circles with halo effect)
- **8 planets + Sun + Moon** — positions computed from Kepler orbital elements updated in real time
- **Alt/Az coordinate grid** — altitude and azimuth lines at configurable intervals
- **Two rendering modes** — AR mode overlays the sky on the live camera preview; Map mode shows the full sky on a dark background
- **Tap to select** any star, planet, constellation, or DSO to open an information panel

The sky renderer (`SkyCanvasView.java`) uses a 3D orthonormal basis aligned with the device orientation. Objects are projected via gnomonic projection centered on the current pointing direction. Rendering is performed on a background thread, with Canvas 2D drawing for the primary view and an OpenGL ES surface (`SkyGLSurfaceView`) available for shader-based effects.

---

### 2. Pinch-to-Zoom & Manual Pan

- **Pinch gesture** adjusts the field of view from 20° (narrow, telephoto-like) to 120° (wide, full-sky overview)
- **Manual pan mode** — when enabled in Settings, finger drag overrides sensor tracking. The sky stays locked to wherever you dragged it; double-tap resets to sensor-aligned view
- **Smart selection UI** — tapping in a dense region shows:
  - A compact chip strip (2–4 nearby objects) for quick selection
  - An expandable bottom sheet (5+ objects) with a scrollable list
- Selection reticle pinpoints the exact tap location before resolving to the closest object

---

### 3. Planet Trajectories

Long-press any planet to visualise its orbital path:

- **60-day trajectory** sampled every 2 days, rendered as a curved polyline across the sky
- **Date labels** at regular intervals along the path so you can see where the planet will be on a specific night
- **Trajectory lock-on** — the overlay remains visible while you pan or rotate; previously the trajectory disappeared as soon as the planet left view
- **Full orbit span option** — Mercury's 88-day and Jupiter's 4,332-day orbits rendered at appropriate densities
- **Unlock button** appears on-screen during lock mode; tapping returns to normal sensor tracking
- **Snap-to behaviour** — entering lock mode smoothly animates the view to centre on the locked planet
- Orbital mechanics use Kepler's equations via `Universe.getRaDec(body, date)` (Kotlin, `core/control/space/Universe.kt`)

---

### 4. Tonight's Highlights

- One-tap summary of every notable object visible from your current location and time
- **Planets** — filtered to those with altitude > 5°
- **Bright stars** — magnitude threshold applied; sorted by altitude
- **Messier objects** — filtered to altitude > 10°
- **Constellations** — filtered to altitude > 20° (enough of the figure above the horizon to be recognisable)
- All results respect the `TimeTravelClock` — Tonight's Highlights for any date you've set in Time Travel
- Tap any item in the panel to animate the sky map to that object

---

### 5. Time Travel

- A date/time slider lets you move to any moment in history or the future
- All orbital calculations — planet positions, rise/set times, and Tonight's Highlights — use `TimeTravelClock.getTimeInMillisSinceEpoch()` transparently
- Drag the slider to watch planets move in real time
- Useful for planning observations (upcoming oppositions, conjunctions, eclipses) or exploring historical skies

---

### 6. AstroBot AI Chat

An AI astronomy assistant powered by the OpenAI Chat Completions API, accessible via the chat button in the FAB menu.

**Capabilities:**
- Answer any astronomy question — object identification, observation planning, orbital mechanics, mythology, history
- **Context-aware**: the system prompt automatically includes your GPS coordinates, current time, sky map pointing direction (RA/Dec), and the name of any selected object
- **Dynamic suggestion chips** on open — shows the currently selected sky object first, followed by planets currently above the horizon (computed from live ephemeris in the background), then static astronomy fallback prompts
- **Follow-up question chips** returned with every response; tap to ask immediately
- **Typing indicator** shown while waiting for the API response
- **Retry button** on failed messages — resend without re-typing after a network or API error
- **Clear chat** button to wipe the conversation history

**API Key:**
- Enter your OpenAI API key in the Settings screen
- Stored locally in `EncryptedSharedPreferences` (AndroidX Security library) — never transmitted except directly to the OpenAI API

---

### 7. Sky Brightness Meter

Estimates light pollution at your observing site from a sky photograph.

- Load any sky photo in the Plate Solve screen and tap **Sky Quality**
- Two-pass analysis:
  - **EXIF metadata** — reads ISO, shutter speed, and aperture to normalise for exposure
  - **Pixel luminance statistics** — mean brightness, highlight clipping fraction, sky-region sampling
- Classifies the result on the **Bortle dark-sky scale** (class 1 = pristine dark sky, class 9 = inner-city sky glow)
- `BortleScaleView` renders a colour-coded 1–9 gauge (green through red) with an animated needle indicating your class
- Result displayed with class number, label (e.g., "Class 4 — Rural/Suburban Transition"), and descriptive text
- Result is cached per loaded image — reopening the dialog is instant

---

### 8. Image Stacking

Combines multiple short-exposure frames of the same sky region to improve signal-to-noise ratio, revealing fainter stars and nebulosity invisible in a single exposure.

**Why triangle matching instead of FFT?**
FFT phase correlation (used by reference projects like `android_live_stacker`) handles translation only. A handheld phone rotates between exposures, so translation-only alignment fails. Triangle asterism matching uses scale-invariant side-length ratios, making it invariant to rotation, scale, and translation simultaneously.

**Pipeline (per added frame):**
1. `bitmapToGrayscale()` — convert frame to grayscale float array
2. `detectStarsNative()` — simplexy star detection, reused from plate solving (adaptive plim retry, edge filtering, hot pixel removal)
3. **Triangle asterism matching** — form triangles from the top 50 detected stars using each star's 5 nearest neighbours (k-d tree search via libkd). Compute invariant ratios `(s1/s0, s2/s0)` for sorted side lengths. Match ratios between the reference frame and the new frame.
4. **RANSAC affine estimation** — 100 iterations; pick 3 random correspondences and solve the 6-parameter affine system (GSL LU decomposition). Count inliers at a 3 px reprojection threshold. Retain the best-fit transform.
5. **Bilinear warp** — transform the new frame into the reference frame's coordinate system
6. **Mean accumulation** — float-precision sum across all warped frames, divided by frame count on readout
7. **Preview update** — display the current stack after each frame

**Performance:** SNR improves approximately √N for N frames (shot noise limited). Memory: grayscale-only accumulation at float precision uses approximately 20 MB for a 1920×2560 image.

**Configuration:**
- Up to 10 frames per stack
- Star detection: plim=8.0 (adaptive retry to 6.0 → 4.0), dpsf=1.0, downsample computed adaptively (4× for >8 MP, 2× for ≥2 MP, 1× otherwise)
- Alignment: top 50 brightest stars, 5 nearest neighbours per star, RANSAC 100 iterations, 3 px inlier threshold
- No additional dependencies — reuses libkd and gsl-an from the astrometry integration

---

### 9. Plate Solving

Identifies the precise coordinates of any photo of the sky using astrometry.net compiled as a native library via JNI.

**What it does:**
- Detects stars in the image using the simplexy algorithm (a matched-filter peak-finding approach used by the astrometry.net `solve-field` tool)
- Performs blind astrometric plate solving using 10 bundled FITS index files (scales 4115–4119), returning the WCS (World Coordinate System) transformation matrix
- Reports right ascension and declination of the image centre, plus pixel scale in arcseconds/pixel

**Accuracy against reference image (1920×2560 Orion field):**
- 677 stars detected (±50)
- Solved: RA = 81.37°, Dec = −0.99° (within 0.5° of `solve-field` reference)
- Solved at depth 21–30, logodds = 140.52

**Star ordering — resort + uniformize:**
`solve-field` applies three reordering steps before solving. These are replicated in the JNI code:
1. **Resort** — interleave flux-sorted and raw-signal-sorted permutations (matching `resort-xylist.c`)
2. **Uniformize** — divide the image into a 10×10 spatial grid, round-robin interleave stars from each bin so the first N stars span the entire field (not clustered in one bright region)
3. **Cut** — optional truncation to max star count

Without uniformize, the first ~20 stars cluster in the Orion Nebula region. The solver cannot form field-spanning quads and never finds a match. With uniformize, the first 9 stars span the full field and the solver converges at depth 21–30.

**Adaptive star detection:**
- If fewer than 30 stars are detected, the detection is retried with progressively lower plim thresholds (8.0 → 6.0 → 4.0), each retry strictly non-increasing from the initial value
- Post-detection edge margin filtering removes stars too close to image boundaries
- Hot pixel filtering rejects detections whose flux exceeds 50× the median flux of all detections

**Index files:** Bundled in Android assets (offline, no network required). Files 4115–4119 cover angular scales from roughly 10 to 180 arcseconds per pixel.

---

### 10. Star & Object Information

Tapping any object on the sky map opens a detail panel showing:

- **Stars**: visual magnitude, spectral type (e.g., K2 III), distance in light-years, RA/Dec coordinates, parent constellation
- **Planets**: current altitude and azimuth, distance from Earth (AU), phase (where applicable), next rise/set times
- **Constellations**: abbreviation, genitive form, area in square degrees, notable stars, mythology summary
- **Deep sky objects**: Messier and NGC numbers, object type, distance, angular size, visual magnitude

Related stars and objects are listed at the bottom of each panel for further exploration.

---

### 11. Search & Navigate

- Full-text search over the complete star catalog, all 8 planets, 88 constellations, and 110 Messier objects
- **Autocomplete** — results appear as you type, ranked by relevance and visual magnitude
- **Navigation arrow** — after selecting a search result, a directional arrow appears on screen pointing toward the target; it auto-dismisses when the object is centred on screen
- Search accessible via the search button in the FAB menu or the search FAB on the sky map

---

### 12. Night Mode

- Switches the entire UI to a deep-red colour theme to preserve dark adaptation
- Red light has minimal impact on rhodopsin (the eye's low-light photoreceptor) compared to white or blue light
- Toggle from Settings or from the Night Mode tooltip in the sky map onboarding sequence
- All text, icons, and the sky map renderer respect the night mode state

---

### 13. 3D Compass

- A physical compass widget on the sky map shows cardinal directions (N, S, E, W) relative to device azimuth
- The compass needle tilts in **3D** based on device pitch (looking up tilts the compass perspective to match), giving a spatially accurate sense of orientation
- Pitch-based camera transform applied to the compass canvas using a perspective projection
- Tilt is clamped to avoid visual artefacts when pointing straight up or down
- Renders at 60 fps via `CompassView.java` (Canvas 2D with hardware acceleration)

---

### 14. Onboarding & Tutorials

A comprehensive first-use experience covering every feature in the app.

**Intro walkthrough (OnboardingActivity — 12 pages):**
1. Welcome
2. Real-Time Sky Map
3. Constellations & Grid
4. Time Travel
5. Planets & DSOs
6. Pinch & Drag
7. Tap to Identify
8. Search & Navigate
9. Star Detection (Plate Solving)
10. Image Stacking
11. AI Sky Assistant
12. Start Exploring

**In-app tooltip sequences (per screen):**

| Screen | Steps | Notable Tooltips |
|--------|-------|-----------------|
| Sky Map | 9 | Welcome, Drag & Zoom, Constellations, Grid, Time Travel, Night Mode, Tonight, Settings, Detect FAB |
| Settings | Dedicated sequence | Key settings explained |
| Search | Dedicated sequence | Search bar, autocomplete, navigation arrow |
| Chat (AstroBot) | Dedicated sequence | Context chips, message input, follow-up suggestions |
| Detect / Stack | Dedicated sequence | Tips dialog → example dialog → detect tooltips |

**Tooltip system features:**
- **Punch-through highlight** — a circular cutout in the dark scrim shows the actual UI element, making tooltips interactive (you can tap the highlighted button during the tutorial)
- **Interactive mode** — certain tooltips require you to interact with the highlighted element before advancing (e.g., tapping the FAB to expand it)
- **Multi-highlight** — a single tooltip can highlight multiple elements simultaneously
- **Auto-fallback positioning** — tooltips automatically switch ABOVE↔BELOW or LEFT↔RIGHT if they would go off-screen; a final clamp to screen margins ensures nothing is clipped
- **Screen reader support** — tooltips announce their text via `AccessibilityManager.announceForAccessibility()` for TalkBack users
- **Step counter** — "Step N of M" indicator shown in each tooltip
- **Builder API** — `TooltipConfig.Builder` enables concise per-screen configuration

---

### 15. Intro Splash Screen

A custom animated splash screen built entirely with Canvas 2D (no GIFs or videos):

- **220 procedurally placed stars** rendered with per-instance twinkle animation (sin wave with random phase and frequency); brighter stars have radial gradient glows
- **Shooting star / meteor pool** — 6–8 concurrent meteors spawn every 0.3–0.8 seconds, each with a fading tail rendered as a gradient-stroked line
- **"Tap anywhere to explore"** hint text fades in after 1.5 seconds with a subtle infinite alpha-pulse animation
- **Warp tunnel transition** — tapping the splash triggers a 3-second warp effect: stars streak outward from the centre, accelerate with an ease-in curve, shift colour toward blue as speed increases, then a white flash fades to the sky map
- **Smooth circular reveal** — the sky map is covered by an overlay that performs a circular reveal animation (`ViewAnimationUtils.createCircularReveal`) after the warp completes; the tooltip onboarding sequence is deferred to `onAnimationEnd` to avoid visual overlap
- Warp-complete events are preserved across view detach/attach cycles via a `pendingWarpComplete` flag, preventing stuck onboarding on slow devices

---

### 16. Constellation Identification

- Tap any region of the sky (not just a labelled constellation centroid) to identify which constellation you are inside
- `ConstellationBoundaryResolver` implements the IAU official boundary algorithm: each IAU boundary segment is stored as a right-ascension range at a given declination epoch, and the resolver performs point-in-polygon tests in equatorial coordinates
- The resolved constellation name is shown in the tap-to-select panel and in the AstroBot system prompt context

---

### 17. Educational Content

Every object in the app is backed by a structured data store providing rich educational content:

- **Stars** — spectral classification explained, distance context (light travel time), notable companions, mythology origin of the name
- **Constellations** — IAU-recognised boundaries, area ranking, best viewing season, notable deep sky objects within, Greek/Roman/Arabic mythology with cultural notes
- **Planets** — orbital parameters (semi-major axis, eccentricity, inclination), physical data (radius, mass, moons), current apparition description, historical discovery notes
- **Deep sky objects** — discovery history, distance measurement methods, visual appearance at different apertures, photography tips

Data is stored in Protocol Buffer binary catalogs for fast random access without a database. The education detail screen (`EducationDetailActivity`) renders Markdown-formatted content with expandable sections.

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Languages | Java (primary), Kotlin (astronomy engine) |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 34 (Android 14) |
| Architecture | MVVM + Dagger 2 dependency injection |
| UI framework | AndroidX, Material Design 3, ConstraintLayout, MotionLayout |
| Camera | CameraX (ImageCapture + Preview) |
| Sky rendering | Canvas 2D (`SkyCanvasView`) + OpenGL ES (`SkyGLSurfaceView`) |
| Star data | Protocol Buffers (binary Hipparcos + Messier catalogs) |
| Native layer | NDK 25.1, CMake 3.22.1, astrometry.net C sources |
| Native ABIs | arm64-v8a, armeabi-v7a |
| AI / API | OpenAI Chat Completions API (GPT) |
| Security | `androidx.security:security-crypto` — EncryptedSharedPreferences for API key storage |
| Location | Google Play Services — Fused Location Provider |
| Image loading | Glide |
| Dependency injection | Dagger 2 |
| Build | Gradle 8 + Kotlin DSL |

**Native C libraries compiled from source:**
- `simplexy` / `image2xy` — star detection (matched-filter peak finding)
- `solver` / `verify` / `tweak2` — plate solving and WCS fitting
- `libkd` — k-d tree for triangle matching and star nearest-neighbour search
- `qfits-an` — FITS file I/O for index files
- `gsl-an` — linear algebra (LU decomposition for RANSAC affine solve)

---

## Requirements

| Requirement | Detail |
|-------------|--------|
| Android version | 8.0+ (API 26+) |
| Device type | Physical device required (camera + motion sensors) |
| Permissions | Camera, Location (fine), Internet (AstroBot only) |
| Storage | ~50 MB for app + index files |
| RAM | 2 GB+ recommended (image stacking uses ~20 MB float buffers) |
| OpenAI API key | Optional — required only for AstroBot AI chat |
| Internet | Optional — required only for AstroBot; all astronomy functions work offline |

---

## Build Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1) or newer
- NDK 25.1.8937393 (install via SDK Manager → SDK Tools → NDK)
- CMake 3.22.1 (install via SDK Manager → SDK Tools → CMake)
- JDK 17

### Debug APK

```bash
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk` (~13.7 MB).

### Install on Connected Device

```bash
./gradlew installDebug
```

### Run Unit Tests

```bash
./gradlew test
```

### Release Build

```bash
./gradlew assembleRelease
```

Requires a signing keystore configured in `app/build.gradle` or via environment variables.

### Windows (from WSL or cmd.exe)

```bat
gradlew.bat assembleDebug
```

Or from WSL:

```bash
cmd.exe /c "cd /d D:\path\to\Astro-Mobile-App && gradlew.bat assembleDebug"
```

---

## How to Use

### First Launch

1. Grant camera and location permissions when prompted
2. Complete the 12-page onboarding walkthrough (or skip to jump straight in)
3. Tap anywhere on the splash screen to enter via the warp tunnel transition
4. Follow the 9-step tooltip tutorial on the sky map

### Core Sky Map

1. Hold your phone up toward the sky
2. Move the phone to pan the view — the sky map tracks your orientation in real time
3. Tap any object to open its information panel
4. Pinch to zoom in (narrow FOV) or out (wide FOV)
5. Tap the bottom bar toggles to show or hide constellation lines, planets, the coordinate grid, and deep sky objects

### FAB Menu

Tap the floating action button (bottom-right) to expand four sub-actions:

| Button | Action |
|--------|--------|
| Search | Open star/planet/constellation search |
| Detect | Open plate solving screen |
| Chat | Open AstroBot AI chat |
| Stack | Open image stacking screen |

The FAB is draggable — long-press and drag to reposition it. Position is saved across sessions.

### Plate Solving

1. Tap **Detect** in the FAB menu
2. Load a photo from your gallery or capture one via CameraX
3. Tap **Solve** — star detection and plate solving run on a background thread
4. The solved RA/Dec coordinates are displayed with a confirmation overlay on the image
5. Tap **Sky Quality** to run the Bortle scale light pollution analysis on the same image

### Image Stacking

1. Tap **Stack** in the FAB menu
2. Capture or import 2–10 frames of the same sky region
3. Tap **Process** — each frame is star-detected, aligned, warped, and accumulated
4. The stacked result is displayed; tap **Plate Solve** to identify the stacked image's coordinates

### AstroBot

1. Tap **Chat** in the FAB menu
2. Type any astronomy question or tap a suggestion chip
3. The bot's response appears with follow-up question chips below
4. Context (location, sky pointing, selected object) is injected automatically — no need to describe what you're looking at

### Planet Trajectories

1. Select a planet on the sky map (tap it to open the info panel)
2. Long-press the planet to display its 60-day trajectory
3. Pan the sky map — the trajectory stays locked on screen
4. Tap **Unlock** (top-right) to return to normal tracking

### Time Travel

1. Tap the clock/calendar icon in the bottom control bar
2. Drag the date/time slider to move forward or backward in time
3. Planets, Tonight's Highlights, and all calculations update in real time
4. Tap **Now** to return to the current time

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Android App (Java/Kotlin)                      │
│                                                                          │
│  ┌───────────────────────────┐   ┌──────────────────────────────────┐   │
│  │  UI Layer                 │   │  Data / Domain Layer             │   │
│  │  SkyMapActivity           │   │  Universe.kt (Kepler orbits)     │   │
│  │  PlateSolveActivity       │   │  StarRepository (Proto buffers)  │   │
│  │  ImageStackingActivity    │   │  OpenAIClient (Chat API)         │   │
│  │  SkyBrightnessActivity    │   │  ConstellationBoundaryResolver   │   │
│  │  ChatBottomSheetFragment  │   │  TimeTravelClock                 │   │
│  │  OnboardingActivity       │   │  SkyBrightnessAnalyzer           │   │
│  │  IntroSplashActivity      │   └──────────────────────────────────┘   │
│  └───────────────────────────┘                                          │
│                                                                          │
│  ┌───────────────────────────┐   ┌──────────────────────────────────┐   │
│  │  Rendering                │   │  Native Bridge (JNI)             │   │
│  │  SkyCanvasView (Canvas2D) │   │  AstrometryNative.java           │   │
│  │  SkyGLSurfaceView (GLES)  │   │  StackingNative.java             │   │
│  │  StarFieldView (splash)   │   │  NativePlateSolver.java          │   │
│  │  CompassView              │   │  ImageStackingManager.java       │   │
│  │  BortleScaleView          │   └──────────────────────────────────┘   │
│  └───────────────────────────┘                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │ JNI
┌───────────────────────────────────▼─────────────────────────────────────┐
│                    libastrometry_native.so (C, NDK)                      │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐   │
│  │  Star Detection  │  │  Plate Solver    │  │  Image Stacking      │   │
│  │  simplexy.c      │  │  solver.c        │  │  stacking_jni.c      │   │
│  │  image2xy.c*     │  │  verify.c        │  │  - triangle match    │   │
│  │  dallpeaks.c     │  │  tweak2.c        │  │  - RANSAC affine     │   │
│  │  dsigma.c        │  │  fit-wcs.c       │  │  - bilinear warp     │   │
│  │  ctmf.c          │  │  index.c         │  │  - mean accumulator  │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────────┘   │
│                                                                          │
│  Support: libkd (k-d tree)  |  gsl-an (linear algebra)  |  qfits-an    │
│  * image2xy.c has upstream assertion bug fix (s->image_u8 = NULL)       │
└─────────────────────────────────────────────────────────────────────────┘
```

**Dependency injection:** Dagger 2 component graph wires repositories, ViewModels, and use-cases. The `AppComponent` scoped to the Application provides the star repository, location service, and clock singletons; per-activity subcomponents provide ViewModels and camera managers.

---

## Native Library (JNI)

The native library (`libastrometry_native.so`) is compiled from astrometry.net C sources for `arm64-v8a` and `armeabi-v7a` using NDK 25.1 + CMake 3.22.1.

### Key modifications from upstream astrometry.net

| File | Modification | Reason |
|------|-------------|--------|
| `image2xy.c` (line 68) | Added `s->image_u8 = NULL;` after `upconvert()` | Fixes assertion crash when downsampling u8 images; upstream bug |
| `astrometry_jni.c` | Resort + uniformize star reordering | Replicates `solve-field`'s three-step ordering; required for field-spanning quads |
| `astrometry_jni.c` | `verify_pix = 1.0` (was 2.0) | Match `solve-field` default |
| `astrometry_jni.c` | `tweak_aborder = 2` (was 3) | Match `solve-field` default |
| `astrometry_jni.c` | Adaptive plim retry | Recover star count on low-contrast images |
| `astrometry_jni.c` | Hot pixel filtering (50× median flux) | Remove detector artefacts before solving |

### Building and testing natively (WSL)

```bash
# Run the authoritative WSL test against the reference image
gcc -o test_solve_wsl /mnt/d/Download/DIP/test_solve_wsl.c \
    -I/usr/local/astrometry/include \
    -I/mnt/d/Download/DIP \
    -L/usr/local/astrometry/lib \
    -lastrometry -lm -lpthread
./test_solve_wsl /mnt/d/Download/DIP/img.png \
    /mnt/d/Download/DIP/astrometry-indexes/

# Expected output:
# Stars detected: 667
# SOLVED at depth 31-40
# RA  = 81.3673
# Dec = -0.9890
# logodds = 125.15
```

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete week-by-week history.

**Recent releases:**

| Version | Date | Highlights |
|---------|------|-----------|
| Week 9 | 2026-03-17 | Onboarding overhaul, 9 sky map tooltips, draggable FAB, adaptive star detection, 18 bug fixes |
| Week 8 | 2026-03-12 | Starfield splash, image stacking pipeline, radial FAB menu, constellation boundaries, 12-page onboarding |
| Week 7 | 2026-02-23 | AstroBot AI chat, sky brightness meter, planet trajectory lock-on, 3D compass tilt |
| Week 6 | 2026-02-15 | 3D compass, tooltip tutorial system, multi-page onboarding |
| Week 5 | 2026-02-10 | Pinch-to-zoom, planet trajectories, Messier catalog, Tonight's Highlights |
| Week 4 | 2026-02-03 | On-device plate solving (astrometry.net JNI) |
| Week 3 | 2026-01-27 | Constellation detection from images, star/constellation education |
| Week 2 | 2026-01-20 | Star search, navigation arrow |
| Week 1 | 2026-01-13 | Initial AR sky map with Hipparcos stars, 88 constellations, planets |

---

## Credits

- **Astronomy engine** — core orbital calculations adapted from [Sky Map (stardroid)](https://github.com/sky-map-team/stardroid) under the Apache 2.0 License
- **Star catalog** — Hipparcos Input Catalogue (ESA, 1997); 60,000+ stars with positions, magnitudes, and spectral types
- **Plate solving** — [astrometry.net](https://astrometry.net/) (D. Lang, D. Hogg, et al.), compiled from source via NDK; FITS index files generated from 2MASS
- **Messier catalog** — public domain astronomical data
- **IAU constellation boundaries** — Delporte (1930) boundary data via the IAU Working Group on Star Names
- **AI assistant** — OpenAI Chat Completions API

---

## License

This project is developed for educational purposes.

Core astronomy engine adapted from [stardroid](https://github.com/sky-map-team/stardroid) under the **Apache License 2.0**.

Astrometry.net sources compiled under the **GNU General Public License v2** (or later). FITS index files are freely redistributable for non-commercial use.

Feel free to fork and learn from this project.

---

*For developer documentation, architecture decisions, and bug history, see the [docs/](./docs/) folder and [CHANGELOG.md](CHANGELOG.md).*
