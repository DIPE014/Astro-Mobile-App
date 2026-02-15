# Release Notes - Week 6: Onboarding, Compass & Bug Fixes

**Branch:** `dev/week6`
**Date:** 2026-02-15

---

## New Features

### 1. 3D Rotating Compass
- Animated compass widget rendered on the sky map screen
- Shows cardinal directions (N, S, E, W) relative to device orientation
- Smooth rotation animation that tracks azimuth from device sensors
- Visible at all times for spatial orientation reference

### 2. In-App Tooltip Tutorial System
- Step-by-step walkthrough triggered on first launch
- 6 tooltips guiding users through key features:
  1. Welcome message
  2. Manual scroll & pinch-to-zoom explanation
  3. Constellation toggle (highlighted)
  4. Time Travel button (highlighted)
  5. Search button (highlighted)
  6. Star Detection / camera button (highlighted)
- Dark scrim overlay with circular punch-through highlighting the target button
- Skip and Next navigation buttons positioned inside the tooltip bubble
- Completion state saved to SharedPreferences (shows only once)

### 3. Onboarding Walkthrough
- Multi-page onboarding screen for new users
- Star detection tips and feature overview
- Integrates with the tooltip tutorial for a cohesive first-run experience

---

## Bug Fixes

### Manual Drag Mode - Rendering Fixed
**Root cause:** All 28 `projectToScreen()` calls in `SkyCanvasView` hardcoded `altitudeOffset`/`azimuthOffset` (sensor values). When the user dragged in manual mode, `manualAzimuth` and `manualAltitude` were updated but never used for rendering, so the sky appeared frozen.

**Fix:** Updated `getViewAzimuth()` and `getViewAltitude()` to return manual values when `isManualMode` is true. All projection calls now go through these methods.

**Files:** `SkyCanvasView.java`

### Manual Mode Settings Sync
**Root cause:** `SettingsViewModel` is per-activity. When the user toggled manual scroll OFF in `SettingsActivity`, it wrote to SharedPreferences but only updated that activity's ViewModel. `SkyMapActivity`'s ViewModel still had the stale `true` value, so the observer never fired and manual mode stayed active.

**Fix:** `onResume()` now re-reads the manual scroll preference directly from SharedPreferences and syncs `SkyCanvasView` state. Also removed the deprecated "Manual Mode - Tap to reset" banner (manual mode is now settings-only).

**Files:** `SkyMapActivity.java`

### Tooltip Highlight Not Working
**Root cause:** `PorterDuff.Mode.CLEAR` requires an offscreen buffer to punch through the scrim correctly. Without a layer type set, the CLEAR paint draws black instead of transparent.

**Fix:** Added `setLayerType(LAYER_TYPE_SOFTWARE, null)` in `TooltipView.init()`.

**Files:** `TooltipView.java`

### Info Panel Close Button Overlapping FABs
**Root cause:** The info panel's end was constrained to `parent`, making it span the full screen width. The close button at the panel's top-right corner overlapped with the search and camera FABs on the right edge.

**Fix:** Changed constraint from `layout_constraintEnd_toEndOf="parent"` to `layout_constraintEnd_toStartOf="@id/fabSearch"` so the panel stops before the FABs.

**Files:** `activity_sky_map.xml`

### Tutorial Marked Completed Prematurely
**Root cause:** If anchor views weren't found during `showTooltipTutorialIfNeeded()`, the tooltip list could be empty. `start()` would return, but if any tooltips did get added and then `showTooltip()` advanced past the list, `finish()` would mark the tutorial as completed even if nothing was shown.

**Fix:** `start()` now returns without calling `finish()` when the tooltip list is empty. Also added `showTooltipTutorialIfNeeded()` to the `initializeSkyMapOnly()` path (camera permission denied).

**Files:** `TooltipManager.java`, `SkyMapActivity.java`

### Build System - Cross-Platform Java Path
**Root cause:** Hardcoded Java path in build scripts failed on different machines.

**Fix:** Replaced with robust cross-platform Java path detection.

**Files:** Build configuration

---

## Files Added

| File | Description |
|------|-------------|
| `TooltipView.java` | Custom FrameLayout with scrim overlay, circular punch-through highlight, and speech bubble |
| `TooltipManager.java` | Manages tooltip sequence, positioning, navigation, and completion state |
| `TooltipConfig.java` | Configuration model for individual tooltips (anchor, message, position) |
| `CompassView.java` | 3D rotating compass widget with smooth animation |
| `OnboardingActivity.java` | Multi-page onboarding walkthrough |

## Files Modified

| File | Changes |
|------|---------|
| `SkyCanvasView.java` | `getViewAzimuth()`/`getViewAltitude()` now return manual values when in manual mode; all 28 projection calls use these methods |
| `SkyMapActivity.java` | Re-read manual scroll pref in `onResume()`; removed deprecated indicator banner; simplified manual mode listener and AR toggle; added tutorial call to `initializeSkyMapOnly()`; updated tooltip text |
| `TooltipView.java` | Added `setLayerType(LAYER_TYPE_SOFTWARE, null)` for PorterDuff CLEAR support |
| `TooltipManager.java` | Prevent marking completed with empty tooltip list; positioned buttons inside bubble |
| `activity_sky_map.xml` | Info panel end constraint changed from parent to `fabSearch` |

---

## Known Limitations
- Tutorial completion is stored in SharedPreferences; "Clean Project" in Android Studio does not reset it. To replay: clear app data or uninstall/reinstall.
- Compass accuracy depends on device magnetometer calibration
- Tooltip bubble size is fixed (280x140dp); long messages may be clipped on small screens

---

## Testing Notes
- **Tutorial replay:** Clear app data (Settings > Apps > Astro > Clear Data), then relaunch to see the full tutorial
- **Manual drag:** Enable Manual Scroll in Settings, return to sky map, drag to verify sky moves. Disable in Settings, return, verify sensor tracking resumes.
- **Info panel:** Tap a star, verify the close button (X) does not overlap the search/camera FABs
- **Compass:** Rotate device and verify compass tracks smoothly
