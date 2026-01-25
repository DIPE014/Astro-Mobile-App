# Astro-Mobile-App Implementation Plan

## Phase 8: New Features

All 7 initial phases are complete (251 passing tests). This plan covers the next stage.

**AI/ML features excluded** - will be added in a future phase.

---

## Current State

### Completed (Phases 1-7)
- Canvas-based star/constellation rendering
- AR camera mode with CameraX
- Night mode
- Settings activity
- Star tap-to-select with info panel
- 251 unit tests passing

### Placeholder Features (show "Coming Soon")
- Time Travel button (`btnTimeTravel`)
- Planets button (`btnPlanets`)
- Search FAB (`fabSearch`)

### Issue to Fix
- GPS hardcoded to New York (SkyMapActivity line 591)

---

## Branch Strategy

```
trung/test (base)
    ├── feature/gps-tracking ──────> merge to trung/test
    ├── feature/magnitude-control ─> merge to trung/test
    ├── feature/time-travel ───────> merge to trung/test
    ├── feature/planets ───────────> merge to trung/test
    ├── feature/search ────────────> merge to trung/test
    └── feature/constellation-lines > merge to trung/test
```

---

## Feature 1: Real-time GPS Tracking
**Branch:** `feature/gps-tracking`
**Priority:** HIGHEST (foundation for all other features)

### Tasks
- [ ] Wire LocationController to SkyMapActivity via Dagger
- [ ] Implement LocationListener in SkyMapActivity
- [ ] Update SkyCanvasView.setObserverLocation() on GPS change
- [ ] Update AstronomerModel with real location
- [ ] Add GPS status indicator in UI
- [ ] Handle permission flow and fallback to default location

### Files to Modify
- `app/.../ui/skymap/SkyMapActivity.java`
- `app/.../core/renderer/SkyCanvasView.java`
- `app/.../di/AppModule.java`

---

## Feature 2: Time Travel
**Branch:** `feature/time-travel`
**Priority:** HIGH

### Tasks
- [ ] Create `TimeTravelClock.java` (port from stardroid)
  - 13 speed levels: -1 week/sec to +1 week/sec
  - Methods: setTimeTravelDate(), accelerate(), decelerate(), pause()
- [ ] Create `TimeTravelDialog.java` with Material Design 3
  - Date picker, time picker, quick presets
- [ ] Create `TimePlayerView.java` overlay
  - Rewind/Pause/Forward buttons, speed indicator, date display
- [ ] Integrate with SkyMapActivity
- [ ] Update star positions when time changes

### Files to Create
- `app/.../core/control/TimeTravelClock.java`
- `app/.../ui/dialog/TimeTravelDialog.java`
- `app/.../ui/skymap/TimePlayerView.java`
- `res/layout/dialog_time_travel.xml`
- `res/layout/layout_time_player.xml`

### Reference
- Stardroid: `control/TimeTravelClock.java`

---

## Feature 3: Planets Layer
**Branch:** `feature/planets`
**Priority:** HIGH

### Tasks
- [ ] Add planet drawable icons (10 planets)
- [ ] Update SolarSystemBody.kt with real resource IDs
- [ ] Create `PlanetPositionCalculator.java`
- [ ] Create `PlanetsLayer.java`
- [ ] Add planet rendering to SkyCanvasView
- [ ] Implement togglePlanets() in SkyMapActivity

### Files to Create
- `app/.../core/control/PlanetPositionCalculator.java`
- `app/.../core/layers/PlanetsLayer.java`
- `res/drawable/ic_planet_*.xml` (10 icons)

### Reference
- Stardroid: `ephemeris/SolarSystemBody.kt`, `ephemeris/OrbitalElements.kt`

---

## Feature 4: Search with Point-To Arrow
**Branch:** `feature/search`
**Priority:** MEDIUM-HIGH

### Tasks
- [ ] Create `PrefixStore.java` (Trie for fast prefix search)
- [ ] Create `SearchIndex.java` (index stars, constellations, planets)
- [ ] Create `SearchResult.java`
- [ ] Create `SearchActivity.java` with Material search bar
- [ ] Create `SearchResultAdapter.java`
- [ ] Create `SearchArrowView.java` (directional arrow overlay)
- [ ] Integrate with SkyMapActivity

### Files to Create
- `app/.../search/PrefixStore.java`
- `app/.../search/SearchIndex.java`
- `app/.../search/SearchResult.java`
- `app/.../ui/search/SearchActivity.java`
- `app/.../ui/search/SearchViewModel.java`
- `app/.../ui/search/SearchResultAdapter.java`
- `app/.../ui/skymap/SearchArrowView.java`
- `res/layout/activity_search.xml`
- `res/layout/item_search_result.xml`

### Reference
- Stardroid: `search/PrefixStore.kt`, `renderer/SearchArrow.java`

---

## Feature 5: Magnitude Control
**Branch:** `feature/magnitude-control`
**Priority:** MEDIUM

### Tasks
- [ ] Add magnitude slider to SettingsActivity (range 3.0-8.0)
- [ ] Update SettingsViewModel with magnitude LiveData
- [ ] Persist to SharedPreferences
- [ ] Apply to StarsLayer on change

### Files to Modify
- `app/.../ui/settings/SettingsActivity.java`
- `app/.../ui/settings/SettingsViewModel.java`
- `res/layout/activity_settings.xml`

---

## Feature 6: Enhanced Constellation Lines
**Branch:** `feature/constellation-lines`
**Priority:** LOW

### Tasks
- [ ] Add line style options (thickness, opacity, color)
- [ ] Update ConstellationsLayer with configurable styles
- [ ] Add optional glow effect

### Files to Modify
- `app/.../core/layers/ConstellationsLayer.java`
- `app/.../ui/settings/SettingsActivity.java`

---

## Implementation Order

```
Week 1:
  ├── feature/gps-tracking ─────> merge
  └── feature/magnitude-control ─> merge

Week 2:
  ├── feature/time-travel ──────> merge
  └── feature/planets ──────────> merge

Week 3:
  └── feature/search ───────────> merge

Week 4:
  └── feature/constellation-lines > merge
```

---

## Stardroid Reference Files

| Feature | Stardroid Reference |
|---------|-------------------|
| Time Travel | `control/TimeTravelClock.java` |
| Planets | `ephemeris/SolarSystemBody.kt`, `ephemeris/OrbitalElements.kt` |
| Search | `search/PrefixStore.kt`, `renderer/SearchArrow.java` |
| Orbital Math | `math/Astronomy.kt`, `ephemeris/SolarSystemRenderable.kt` |

---

## Verification

After each feature branch merge:
1. Run all unit tests: `./gradlew test`
2. Build debug APK: `./gradlew assembleDebug`
3. Manual test on device
4. Update `docs/PROGRESS.md`
