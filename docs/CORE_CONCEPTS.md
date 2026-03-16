# Core Concepts — Astro Mobile App

Developer reference covering all major subsystems. Read this before modifying any feature.

---

## 1. Celestial Coordinates (RA/Dec)

Right Ascension (RA) and Declination (Dec) form the equatorial coordinate system tied to the celestial sphere — the standard reference frame for astronomy.

**Right Ascension**
- Range: 0° to 360° (equivalently 0h to 24h, where 1h = 15°)
- Measured eastward along the celestial equator from the vernal equinox (the point where the Sun crosses the equator heading north each spring)
- Analogous to longitude on Earth, but fixed to the sky, not the ground

**Declination**
- Range: −90° (south celestial pole) to +90° (north celestial pole)
- Measured north/south from the celestial equator
- Analogous to latitude on Earth

**Key property:** Stars have fixed RA/Dec (barring proper motion over centuries). Planets, the Moon, and the Sun move through RA/Dec space over time and must be recalculated for each moment.

**3D unit vector representation.** Any RA/Dec pair maps to a point on the unit sphere:
```
x = cos(RA) · cos(Dec)
y = sin(RA) · cos(Dec)
z = sin(Dec)
```
All internal coordinate transforms operate on these unit vectors. See `Vector3.kt` and `RaDec.kt`.

---

## 2. Coordinate Transformation Pipeline

The app continuously converts phone sensor readings into a sky-pointing direction, then projects that into screen pixels.

```
Phone IMU sensors
    │
    ▼
Accelerometer  →  gravity vector (local "down")
Magnetometer   →  north vector  (horizontal component)
    │
    ▼  cross-product + normalization
3×3 Rotation Matrix  (phone body frame → world frame: East/North/Up)
    │
    ▼  SensorController.java
AstronomerModelImpl.java
    │  applies time + observer latitude/longitude
    ▼  CoordinateManipulations.kt
Phone pointing vector in Equatorial (RA/Dec) frame
    │
    ▼  SkyCanvasView.java
Project RA/Dec → Alt/Az (see §4) → screen pixel
```

**Rotation matrix construction:**
1. Accelerometer gives the gravity vector `g` (pointing down in world frame). Normalize to get the local "up" direction.
2. Magnetometer gives a raw north vector `m`. Project out the vertical component: `north = m − (m·up)·up`, then normalize.
3. East = north × up (cross product).
4. These three orthonormal vectors form the columns of the world-to-phone rotation matrix.

The matrix is maintained live as sensors fire. `AstronomerModelImpl.java` wraps it with time and location to produce the final equatorial pointing used for rendering.

---

## 3. Local Sidereal Time & Julian Date

**Julian Day Number (JD)**

A continuous count of days since noon UT on 1 January 4713 BC (Julian proleptic calendar). Avoids the irregularities of the Gregorian calendar (leap years, month lengths, etc.) and makes day-difference calculations a single subtraction. The current JD is approximately 2,460,000.

Conversion from Unix epoch milliseconds (used in `TimeUtils.kt`):
```
JD = (unix_ms / 86400000.0) + 2440587.5
```

**Greenwich Mean Sidereal Time (GMST)**

GMST is the hour angle of the vernal equinox at the Greenwich meridian. It advances ~4 minutes per solar day (one extra rotation per year relative to the Sun). Formula from Meeus, *Astronomical Algorithms*, Ch. 12:
```
T = (JD − 2451545.0) / 36525.0        // Julian centuries from J2000.0
GMST (degrees) = 280.46061837
               + 360.98564736629 · (JD − 2451545.0)
               + 0.000387933 · T²
               − T³ / 38710000
```

**Local Sidereal Time (LST)**
```
LST = GMST + observer_longitude_degrees
```
LST is the RA currently on the meridian. Any star with RA = LST is due south at its highest point for that observer at that moment.

**Why this matters:** The conversion between equatorial (RA/Dec) and horizontal (Alt/Az) coordinates depends entirely on LST and observer latitude. Get either wrong and every object appears in the wrong position.

---

## 4. Horizontal Coordinates (Alt/Az)

Alt/Az describes where in the local sky an object appears. Unlike RA/Dec it is observer- and time-dependent.

- **Altitude (Alt):** 0° at the horizon, 90° at the zenith. Negative = below horizon.
- **Azimuth (Az):** 0° = north, 90° = east, 180° = south, 270° = west. Measured clockwise when viewed from above.

**Conversion from RA/Dec:**

1. Compute the Hour Angle: `H = LST − RA`
2. Then:
   ```
   sin(Alt) = sin(Dec)·sin(lat) + cos(Dec)·cos(lat)·cos(H)

   cos(Az)·cos(Alt) = sin(Dec)·cos(lat) − cos(Dec)·sin(lat)·cos(H)
   sin(Az)·cos(Alt) = −cos(Dec)·sin(H)
   ```

This transform is performed in `CoordinateManipulations.kt` and `Astronomy.kt`. The resulting Alt is used for visibility filtering (Alt > 0°), tonight's highlights ranking, and planet trajectory display.

---

## 5. Star Database & Rendering

**Source:** Hipparcos catalog — ~118,000 stars, filtered to ~60,000 for the bundled `stars.binary` (Protocol Buffers format). Each entry contains: HIP number, RA, Dec, visual magnitude, spectral type (B−V color index), and a common name where applicable.

**Magnitude limit:** User-configurable. Default 6.5 (naked-eye limit under dark skies). Stars fainter than the limit are culled before rendering. Brighter (lower magnitude) stars are drawn larger.

**Rendering pipeline (per frame):**
1. `StarRepositoryImpl.java` parses `stars.binary` via `ProtobufParser.java` into `StarData` objects.
2. `StarsLayer.java` passes visible stars to `PointRenderer.java`.
3. Each star's RA/Dec is converted to Alt/Az then projected to a screen pixel via `SkyCanvasView.projectToScreen()`.
4. Point size: `size = base_size × 10^(−0.4 × magnitude)` (inverse log scale matching photometric convention).
5. Color from spectral type / B−V index: O/B = blue-white, A = white, F/G = yellow-white, K = orange, M = red.

**Constellations:** `constellations.binary` stores line-segment pairs (star HIP IDs). `ConstellationsLayer.java` draws them via `LineRenderer.java` when the user's FOV is wide enough to make them useful.

**IAU Boundaries:** `bound.json` and `bound_18.json` contain polygon vertices for all 88 IAU constellation regions. `ConstellationBoundaryResolver.java` performs point-in-polygon tests to identify which constellation a given RA/Dec falls within.

---

## 6. Messier Deep Sky Objects

110 objects in `messier.binary`: galaxies, open clusters, globular clusters, emission nebulae, reflection nebulae, planetary nebulae, and supernova remnants.

**Shape-coded rendering:**
- Galaxies: rotated diamonds (orientation from position angle)
- Open clusters: dashed squares
- Globular clusters: concentric circles
- Nebulae (all types): soft ellipses with a glow effect

**Visibility:** Objects below 10° altitude are suppressed. `MessierRepositoryImpl.java` parses the binary; the layer checks Alt at draw time.

**Education content:** `education_content.json` provides descriptions, distance, apparent size, and discovery metadata linked by Messier number. Accessed via `EducationRepository.java`.

---

## 7. Planet Orbital Mechanics

The app computes planet positions from first principles using Keplerian orbital elements. This avoids bundled ephemeris tables and works for any date within a few centuries of J2000.0.

**Six Keplerian elements** (defined at J2000.0, with century rates):

| Symbol | Name | Units |
|--------|------|-------|
| a | semi-major axis | AU |
| e | eccentricity | dimensionless |
| i | inclination | degrees |
| Ω | longitude of ascending node | degrees |
| ω | argument of perihelion | degrees |
| L | mean longitude | degrees |

Elements and their century rates are defined in `OrbitalElements.kt` for each of the eight planets.

**Position computation** (implemented in `SolarSystemObject.kt`):

1. Advance elements to target date: `element = element_J2000 + rate × T` where T is Julian centuries from J2000.0.
2. Mean anomaly: `M = L − ω` (mod 360°).
3. Eccentric anomaly E from Kepler's equation `M = E − e·sin(E)` — solved via Newton's method (5 iterations typically converge to < 10⁻⁶ rad error).
4. True anomaly: `ν = 2·atan2(√(1+e)·sin(E/2), √(1−e)·cos(E/2))`
5. Heliocentric distance: `r = a·(1 − e·cos(E))`
6. Heliocentric ecliptic rectangular coordinates:
   ```
   x = r·[cos(Ω)·cos(ν+ω) − sin(Ω)·sin(ν+ω)·cos(i)]
   y = r·[sin(Ω)·cos(ν+ω) + cos(Ω)·sin(ν+ω)·cos(i)]
   z = r·[sin(ν+ω)·sin(i)]
   ```
7. Rotate ecliptic → equatorial (obliquity ε ≈ 23.439° at J2000.0).
8. Compute Earth's heliocentric position by the same method.
9. Subtract Earth's position → geocentric equatorial vector.
10. Convert to RA/Dec via `atan2`.

`Universe.kt` is the top-level entry point: `getRaDec(body, date)` returns RA/Dec for any solar system body at any time.

---

## 8. Moon Calculation

The Moon's orbit is too perturbed by the Sun for simple two-body Keplerian elements to give useful accuracy. The app uses a simplified truncated Meeus series (accurate to ~0.5° for typical use, sufficient for naked-eye sky maps).

**Key terms computed:**
- Moon's mean longitude L′ and mean anomaly M′
- Sun's mean anomaly M and equation of center
- Argument of latitude F
- Ecliptic longitude λ and latitude β from the dominant periodic terms (~15 largest from Meeus Chapter 47)
- Ecliptic → equatorial coordinate rotation

**Phase calculation:**
```
elongation = Moon_longitude − Sun_longitude
phase_fraction = (1 − cos(elongation)) / 2
```
0 = new moon, 0.5 = quarter, 1.0 = full moon. Implemented in `Moon.kt` / `EarthOrbitingObject.kt`.

---

## 9. Planet Trajectories

Shows a planet's path across the sky over the next 60 days, allowing users to understand its motion over a season.

**Algorithm (in `SkyCanvasView.java`):**
1. Sample `Universe.getRaDec(body, date)` every 2 days → 30 points.
2. For each point, convert RA/Dec → Alt/Az (using current observer location) → screen pixel via `projectToScreen()`.
3. Draw a polyline connecting the projected points.
4. Label points at 10-day intervals with a formatted date string.

The trajectory updates whenever the time changes (time travel slider or real-time tick). Points below the horizon (Alt < 0°) still appear on the path but the connecting segment becomes dashed to indicate the planet is not visible at that time.

---

## 10. Tonight's Highlights

Summarizes what is worth observing on the current night for the user's location.

**Computation (`TonightsHighlights.java`):**
1. Evaluate Alt at the current time for every planet, every star with magnitude < 2, every Messier object, and every constellation centroid.
2. Apply visibility thresholds:
   - Planets: Alt > 5°
   - Bright stars: Alt > 5°
   - Constellations: Alt > 20° (need sufficient area above horizon to be recognizable)
   - Messier objects: Alt > 10°
3. Moon: always included, with phase fraction and phase name (New / Waxing Crescent / First Quarter / etc.).
4. Sort results by altitude descending — most overhead = easiest to observe, fewest atmospheric layers to look through.
5. Return ranked list → `TonightsHighlightsFragment.java` renders as scrollable cards.

---

## 11. Time Travel System

The time travel system lets the user explore the sky at any past or future moment without modifying system time.

**`TimeTravelClock.java`** wraps a base `Clock` with three modes:

| Mode | Behavior |
|------|----------|
| `REAL_TIME` | Delegates to `System.currentTimeMillis()` |
| `FROZEN` | Returns a fixed timestamp set by the user |
| `OFFSET` | Returns `System.currentTimeMillis() + delta_ms` |

All celestial calculations (`AstronomerModelImpl`, `Universe`, `TonightsHighlights`, trajectory sampling) call `TimeTravelClock.getTimeInMillisSinceEpoch()` — never `System.currentTimeMillis()` directly. This is the single clock contract that makes time travel work transparently everywhere.

**UI flow:** `TimeTravelDialogFragment.java` presents a date/time picker and a continuous slider. Moving the slider sets the offset; selecting a specific date/time sets frozen mode. Tapping "Reset" returns the clock to real-time mode and the sky instantly snaps back.

---

## 12. Gesture System (SkyCanvasView)

`SkyCanvasView.java` handles all touch input for the sky map.

**Pinch-to-zoom:**
- `ScaleGestureDetector` fires `onScale()` with a scale factor.
- `adjustFieldOfView(factor)` multiplies the current FOV by `1/factor`, clamped to [20°, 120°].
- Smaller FOV = magnified view; larger FOV = wide-angle view.
- The `setFieldOfView()` method is the internal hook that gesture code calls.

**Manual pan mode:**
- Activated by a single-finger drag that exceeds an 8 dp movement threshold.
- Drag delta is converted from screen pixels to angular offset using the current FOV and screen dimensions.
- `adjustOrientation(deltaAz, deltaAlt)` updates the view direction.
- Entering pan mode **disables sensor tracking** — the sky no longer follows where the phone points. A visual indicator appears.
- Sensor tracking resumes when the user taps the "re-center" button or shakes the device.

**Smart selection on tap:**
- A tap that stays below the 8 dp threshold triggers object selection, not pan.
- `findNearestPlanet()` and equivalent hit-tests for stars and DSOs run within a touch radius that scales with current FOV.
- Result dispatch:
  - 0 objects in radius: dismiss any open sheet
  - 1 object: open info bottom sheet directly
  - 2–4 objects: show a horizontal chip strip for disambiguation
  - 5+ objects: show a scrollable bottom-sheet list

---

## 13. Search System

**`PrefixStore.java`** — a prefix trie built over all searchable names: star common names and Bayer/Flamsteed designations, constellation names and abbreviations, Messier designations (M1–M110), and planet names. Insert and lookup are O(k) where k is the key length.

**`SearchIndex.java`** — wraps `PrefixStore` with ranking:
1. Exact match (query equals full name): rank 0
2. Prefix match (name starts with query): rank 1
3. Substring or fuzzy match: rank 2

`query(prefix)` returns a ranked `List<SearchResult>`.

**Navigation to a selected object:**
1. User selects a result in `SearchActivity.java`.
2. The target's RA/Dec is looked up from the relevant repository.
3. `raDecToAltAz()` converts to local horizontal coordinates at the current time.
4. `SearchArrowView` is activated: it draws a navigation arrow pointing toward the object.

**`SearchArrowView.java`** — roll-corrected overlay arrow:
- Compute the great-circle bearing from the current pointing direction to the target.
- Account for device roll (phone tilt about the pointing axis) using the cross product of the pointing vector with the "up" sensor direction.
- Draw the arrow at the correct angle. If the target is already within the current FOV, switch from an arrow to a highlight ring drawn around the object.

---

## 14. Image Stacking Pipeline

Image stacking combines multiple short-exposure frames to improve SNR by √N for N frames. This makes faint nebulosity and dim stars visible that are buried in read noise in any single frame.

```
For each frame i:

1. Camera capture
   → Bitmap via CameraX ImageCapture (JPEG from physical shutter)

2. bitmapToGrayscale()  [AstrometryNative.java]
   → Luminance weights: L = 0.299·R + 0.587·G + 0.114·B
   → Output: byte[] (0–255 per pixel)

3. detectStarsNative(plim=8.0, dpsf=1.0, downsample=auto)  [astrometry_jni.c]
   → simplexy: background subtraction → threshold → centroid fitting
   → Adaptive plim retry: 8.0 → 6.0 → 4.0
       (strictly non-increasing; retries only if < 30 stars found)
   → Edge margin filter: exclude stars within 5% of image edge
   → Hot-pixel filter: exclude stars with flux > 50× median flux
   → Resort: interleave flux-sorted and raw-signal-sorted permutations
   → Uniformize: divide into 10×10 spatial grid,
                 round-robin select from bins by descending star count

4. Triangle asterism matching  (frame 0 is the reference frame)
   → Build triangles from top-50 stars via 5 nearest neighbors (libkd k-d tree)
   → Each triangle: scale-invariant ratios (s1/s0, s2/s0)
       where s0 ≥ s1 ≥ s2 are sorted side lengths
       (invariant to rotation, scale, translation)
   → K-d tree lookup in ratio space to find matches between frames
   → Produces a list of candidate star correspondences

5. RANSAC affine estimation
   → 100 iterations; each picks 3 random correspondences
   → Solve 6-parameter affine system (2×3 matrix) via 6×6 linear system (GSL LU)
   → Count inliers: transformed point within 3 px of reference counterpart
   → Keep transform with maximum inlier count

6. Bilinear warp
   → Apply affine transform to map new frame pixels into the reference frame
   → Bilinear interpolation for sub-pixel accuracy

7. Mean accumulation
   → float32 sum buffer (same dimensions as reference frame)
   → Divide sum by N after all frames → 8-bit output bitmap
```

SNR improvement factor: √N. For 9 frames, expect ~3× improvement over a single frame.

---

## 15. Plate Solving Pipeline

Plate solving determines the exact RA/Dec center coordinates, plate scale, and orientation of a captured image by matching detected star patterns against a reference catalog.

```
Input: grayscale bitmap (from camera or stacked result)
         │
         ▼
detectStarsNative()
  → Ordered star list (resort + uniformize, see §14 step 3)
         │
         ▼
solveFieldNative(indexes, scaleLow=10, scaleHigh=180 arcsec/px)
  → Load FITS index files (index-4115 to index-4119, from assets/indexes/)
  → Form quads: sets of 4 nearby stars
      (quad size 10%–100% of field diagonal)
  → Hash each quad to a 4D code
      (scale-invariant ratios of intra-quad distances)
  → K-d tree search in code space against index
  → Candidate match: apply transform image quad → sky quad, then verify
      Count stars within verify_pix=1.0 of predicted positions
      Accept if log-odds ratio > logratio_tokeep = 20.0
  → Iterate solver depth (10, 20, 30, ..., 200 stars considered)
  → On acceptance: refine with SIP polynomial distortion (order 2)
         │
         ▼
Output: RA/Dec center + WCS transformation matrix
  (plate scale arcsec/px, rotation angle, SIP distortion coefficients)
```

**Critical parameters:**

| Parameter | Value | Notes |
|-----------|-------|-------|
| `verify_pix` | 1.0 | Match tolerance in pixels; matches solve-field default |
| `distractor_ratio` | 0.25 | Fraction of stars assumed spurious (noise, hot pixels) |
| `logratio_tokeep` | 20.0 | Log-odds acceptance threshold (~e²⁰ ≈ 5×10⁸ to 1 odds) |
| `tweak_aborder` | 2 | SIP polynomial order; matches solve-field default |

The resort + uniformize ordering from step 3 is critical for solver performance. Without it, the first 20 stars cluster in one bright region (e.g. Orion Nebula), the solver cannot form field-spanning quads, and solving fails at typical depths.

---

## 16. Tooltip System (Onboarding)

The onboarding tooltip system walks new users through key UI elements with an interactive overlay that highlights and describes each control in sequence.

**Components:**

- `TooltipConfig.java` — data class holding: anchor view reference, tooltip text, preferred position (ABOVE/BELOW/LEFT/RIGHT), and optional extra-highlight views that should remain click-through while the tooltip is visible.
- `TooltipManager.java` — owns the ordered list of `TooltipConfig` steps, tracks the current step index, and manages the overlay lifecycle (add/remove from root view).
- `TooltipView.java` — custom View responsible for all drawing and touch interception.

**Rendering:**
- The overlay is added as a `FrameLayout` child of the root view, or of the dialog's decor view when used inside a `DialogFragment`.
- A semi-transparent dimmed layer covers the entire screen.
- A transparent punch-through circle (or rounded rect) is cut out around the anchor's bounding box — the user can see and interact with the highlighted control through the hole.
- The tooltip bubble (rounded rect with an arrow tail) is drawn at the computed position, containing the explanation text.
- A step counter ("2 of 5"), a Skip button, and a Next/Got-it button are rendered within the bubble.

**Coordinate handling:**
- `View.getLocationInWindow()` obtains the anchor's screen-absolute position.
- The overlay's own window offset is subtracted → overlay-relative coordinates.
- This approach works correctly for anchors inside `RecyclerView` cells, `DialogFragment` views, and `BottomSheetDialogFragment`.

**Position auto-fallback:**
- The preferred position comes from `TooltipConfig`.
- If the tooltip bubble would be clipped off-screen after layout: ABOVE ↔ BELOW, LEFT ↔ RIGHT.
- The final position is computed after a layout pass so actual bubble dimensions are known.

**Interactive mode:**
- The overlay intercepts `ACTION_DOWN` and `ACTION_UP` touch events.
- On `ACTION_UP`, the touch point is hit-tested against the anchor rect and any extra-highlight rects.
- If a hit is detected, `performClick()` is called on the matching view — the user can actually use the highlighted control while the tooltip is showing, without dismissing the overlay.

**Completion persistence:**
- Completion state is stored in `SharedPreferences`, keyed by a tutorial ID string.
- On app launch, `TooltipManager` reads the preference and skips the sequence if already completed.
- Settings screen exposes a "Replay Tutorial" option that clears the key.

**Accessibility:**
- On each step show, `View.announceForAccessibility()` is called with the tooltip text.
- TalkBack users hear the tooltip content read aloud without needing to navigate to the overlay view.
