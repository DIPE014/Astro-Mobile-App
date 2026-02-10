# Release Notes - Week 5: Sky Map UX Features

**Branch:** `feat/week5-base`
**Base:** `main` @ `d918769`
**Date:** 2026-02-10

---

## New Features

### 1. Pinch-to-Zoom & Manual Pan Mode
- **Pinch gesture** to zoom the sky view between 20° and 120° field of view
- **Drag gesture** to manually pan the sky, overriding device sensor tracking
- **Double-tap** to exit manual mode and return to sensor-based orientation
- Visual indicator when manual mode is active

### 2. Planet Trajectory Visualization
- **Long-press** any planet to display its orbital trajectory over a 60-day window (±30 days)
- Trajectory rendered as an orange path with time-labeled position markers
- Current position highlighted with a larger dot
- Tap anywhere to dismiss the trajectory overlay

### 3. Improved Object Selection UI
- **Smart selection** based on object count in the reticle:
  - 1 object: Info panel shown directly
  - 2-4 objects: Compact horizontal chip strip at the bottom
  - 5+ objects: Scrollable bottom sheet
- Material Design chips with type icons and object names
- Tap a chip to view that object's details

### 4. Deep Sky Objects (Messier) Layer
- New **"Deep Sky"** toggle in the bottom control bar
- Renders Messier catalog objects from the existing `messier.binary` asset
- Shape-coded icons: diamonds (galaxies), squares (clusters), glowing circles (nebulae)
- DSO objects appear in the reticle selection list when visible

### 5. Tonight's Highlights
- New **calendar button** in the top control bar
- Opens a bottom sheet showing celestial objects currently visible:
  - Planets above 5° altitude
  - Bright stars (magnitude < 1.5) above 5°
  - Constellations above 20°
  - Deep sky objects above 10°
- Grouped by type with color-coded section headers
- Tap any item to navigate the sky map to that object

### 6. Expanded Star Information
- All stars now display their **parent constellation** (using IAU boundary polygon matching)
- Previously only named stars showed constellation data
- New boundary data: `bound.json`, `bound_18.json`, `bound_edges_18.json`, `bound_in_18.json`

### 7. Enhanced Educational Content
- Detailed information for the **100 brightest stars**: temperature, radius, mass, luminosity, history, fun facts
- Expanded constellation education: family, best viewing month, mythology, major stars
- New `full_education_content_100.json` asset with structured star data

---

## Bug Fixes
- **MaterialButton API**: Fixed `setIconTintList()` to `setIconTint()` for Material library compatibility
- **Selection UI flow**: Fixed chip strip not dismissing properly after selection
- **Pan smoothness**: Reduced jitter during manual pan gestures
- **Manual mode reset**: Fixed race condition where sensor updates overrode manual pan

---

## Files Added

| File | Description |
|------|-------------|
| `TonightsHighlights.java` | Computes visible objects for current time/location |
| `TonightsHighlightsFragment.java` | Bottom sheet UI for tonight's highlights |
| `fragment_tonights_highlights.xml` | Layout for highlights bottom sheet |
| `MessierObjectData.java` | Data model for Messier/DSO objects |
| `MessierRepository.java` | Repository interface for DSO data |
| `MessierRepositoryImpl.java` | Implementation parsing `messier.binary` protobuf |
| `ConstellationBoundaryResolver.java` | IAU boundary polygon matching for star→constellation lookup |
| `SolarSystemEducation.java` | Data model for expanded star education content |
| `EducationRepository.java` | Repository for JSON-based educational data |
| `full_education_content_100.json` | Educational data for 100 brightest stars |

## Files Modified

| File | Changes |
|------|---------|
| `SkyCanvasView.java` | Zoom/pan gestures, trajectory rendering, DSO drawing, DSO hit-testing |
| `SkyMapActivity.java` | Selection UI, DSO/highlights wiring, toggle buttons, trajectory hookup |
| `activity_sky_map.xml` | Added DSO toggle and Tonight button |
| `StarInfoActivity.java` | Constellation display for all stars |
| `AppModule.java` | Added MessierRepository DI provider |
| `strings.xml` | New string resources for DSO and highlights |

---

## Known Limitations
- Constellation boundary JSON files (~153 MB total) are loaded on first star tap; may cause brief delay on low-end devices
- Planet trajectory duration is fixed at ±30 days (not user-configurable)
- Tonight's Highlights does not filter by light pollution or weather conditions
- DSO detail view is limited to tap-to-center (no dedicated info panel yet)

---

## Testing Notes
- **On-device testing required** for gesture interactions (pinch, pan, long-press)
- Verify trajectory accuracy by comparing planet paths against a reference planetarium
- Test Tonight's Highlights at different times and locations (both hemispheres)
- Monitor memory usage when DSO layer is enabled alongside all other layers
