# Tetra3 Implementation Progress

## Current Status

**Phase**: 1 (Chaquopy Setup)
**Last Updated**: 2026-02-01
**Last Task Completed**: Initial planning and CLAUDE.md setup
**Next Task**: Add Chaquopy plugin to settings.gradle and app/build.gradle

---

## Phase Checklist

### Phase 1: Chaquopy Setup
- [ ] Add Chaquopy plugin to `settings.gradle`
- [ ] Add Chaquopy plugin to `app/build.gradle`
- [ ] Configure Python dependencies (tetra3, numpy, scipy, Pillow)
- [ ] Configure NDK abiFilters
- [ ] Create `app/src/main/python/__init__.py`
- [ ] Create `app/src/main/python/tetra3_wrapper.py`
- [ ] Test: Build compiles with Chaquopy
- [ ] Commit: `[chore] Add Chaquopy plugin for Python support`

### Phase 2: Camera Capture
- [ ] Add ImageCapture use case to `CameraManager.java`
- [ ] Add `captureImage()` method with callback
- [ ] Add `ImageCaptureCallback` interface
- [ ] Test: Can capture and save image file
- [ ] Commit changes

### Phase 3: PlateSolveService
- [ ] Create `ml/model/SolveStatus.java` enum
- [ ] Create `ml/model/DetectedStar.java`
- [ ] Create `ml/PlateSolveResult.java`
- [ ] Create `ml/PlateSolveCallback.java`
- [ ] Create `ml/PlateSolveService.java`
- [ ] Add `hipparcosId` field to `StarData.java`
- [ ] Add `getStarByHipparcosId()` to `StarRepository.java`
- [ ] Test: Service can call Python and parse response
- [ ] Commit changes

### Phase 4: UI
- [ ] Create `activity_photo_capture.xml` layout
- [ ] Create `activity_plate_solve_result.xml` layout
- [ ] Create `item_detected_star.xml` layout
- [ ] Create `ui/platesolve/PhotoCaptureActivity.java`
- [ ] Create `ui/platesolve/PlateSolveResultActivity.java`
- [ ] Create `ui/platesolve/PlateSolveResultViewModel.java`
- [ ] Create `ui/platesolve/StarOverlayView.java`
- [ ] Register activities in `AndroidManifest.xml`
- [ ] Add inject methods to `AppComponent.java`
- [ ] Test: UI flow works end-to-end
- [ ] Commit changes

### Phase 5: Integration
- [ ] Add "Identify Stars" button to `SkyMapActivity.java`
- [ ] Wire button to launch `PhotoCaptureActivity`
- [ ] Test: Full feature works on device
- [ ] Final commit

---

## Completed Tasks

- [x] Read tetra3_implementation_guide.docx and understand Tetra3 algorithm
- [x] Create implementation plan in CLAUDE.md
- [x] Set up progress tracking (this file)

---

## In Progress

- [ ] Starting Phase 1: Chaquopy Setup

---

## Blocked / Issues

None currently.

---

## Notes for Next Agent

1. **Read CLAUDE.md first** - Contains all build commands, code snippets, and workflow instructions
2. **Tetra3 database** - Need to generate `hip_database_fov85.npz` on a PC with Python, then copy to `app/src/main/assets/tetra3/`
3. **Chaquopy requires NDK** - Make sure Android NDK is installed in Android Studio
4. **App size will increase** - Expected ~150-170MB total after adding Python runtime and Tetra3 database

---

## Commit History

| Commit | Description | Phase |
|--------|-------------|-------|
| (pending) | Add Chaquopy plugin | 1 |
