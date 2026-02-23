# Changelog

All notable changes to **Astro Mobile App** are documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Releases are grouped by weekly sprint. Most recent release appears first.

---

## [Week 7] — 2026-02-23

### Summary
Two major new features — an AI astronomy assistant (AstroBot) and a sky brightness / light-pollution meter — plus significant rendering enhancements to planet trajectory tracking and the 3D compass. Twelve code-quality issues identified in code review were also resolved.

---

### Added

#### AstroBot — AI Astronomy Assistant
- New **chat bottom sheet** accessible via a floating action button on the sky map
- Powered by **OpenAI GPT-5 Nano** via the Chat Completions API; model behaves as an astronomy-focused assistant ("AstroBot")
- **Context-aware responses**: automatically injects the observer's GPS location, current time, sky map pointing direction (RA/Dec), and the currently selected object name into the system prompt
- **Dynamic suggestion chips** on open: shows the selected sky object first, followed by planets currently above the horizon (computed from live ephemeris), then static astronomy fallback prompts — all prioritised for relevance
- **Follow-up question suggestions** returned with every response; rendered as tappable chips below each bot message
- **Typing indicator** ("thinking" bubble) shown while waiting for the API response
- **Retry button** on failed messages (network errors, API errors) so the user can resend without re-typing
- **Clear chat** button to wipe conversation history
- **Encrypted API key storage**: OpenAI API key entered in Settings is stored in `EncryptedSharedPreferences` (androidx.security:security-crypto)
- New Settings screen section for entering and saving the API key
- `INTERNET` permission added to `AndroidManifest.xml`

**New files:**
| File | Description |
|------|-------------|
| `data/api/OpenAIClient.java` | HTTP client for the OpenAI Chat Completions endpoint; SSE streaming + non-streaming fallback; structured JSON response parsing |
| `data/model/ChatMessage.java` | Chat message model with role, content, timestamp, thinking/error/followup state |
| `ui/chat/ChatBottomSheetFragment.java` | AstroBot chat bottom sheet: dynamic chips, message input, RecyclerView, observer context injection |
| `ui/chat/ChatMessageAdapter.java` | RecyclerView adapter rendering user and bot message bubbles, follow-up chips, retry action |
| `ui/chat/ChatViewModel.java` | MVVM ViewModel; manages conversation history, dispatches API calls on a single-thread executor, exposes LiveData |

**New layouts / drawables:**
- `fragment_chat_bottom_sheet.xml` — Chat UI with chip scroll view, RecyclerView, and input row
- `item_chat_message_user.xml` — User message bubble (right-aligned)
- `item_chat_message_bot.xml` — Bot message bubble with follow-up chip row and retry/thinking states
- `bg_chat_bubble_user.xml`, `bg_chat_bubble_bot.xml` — Chat bubble backgrounds
- `bg_circle_primary.xml`, `bg_bottom_sheet_handle.xml` — Supporting drawables

---

#### Sky Brightness Meter — Light Pollution Analysis
- Dedicated **Sky Brightness Activity** reachable from the Plate Solve screen after loading an image
- Analyses sky photos to estimate **Bortle dark-sky class** (1 = pristine dark sky → 9 = inner-city sky)
- Two-pass analysis combining:
  - **EXIF metadata** (ISO, shutter speed, aperture) to infer exposure level
  - **Pixel luminance statistics** (mean brightness, highlight clipping, sky-region sampling) from the loaded bitmap
- **BortleScaleView** — custom canvas view rendering a colour-coded 1–9 gauge with the current class highlighted
- Results displayed with Bortle number, class label, and descriptive text
- Sky quality page integrated into the **onboarding walkthrough** (new users see an explanation of the Bortle scale)
- **Sky brightness dialog** in `PlateSolveActivity` — after loading a plate-solve image the user can tap a "Sky Quality" button to see an inline result without leaving the activity
- Result caching: analysis runs once per loaded image; re-opening the dialog is instant

**New files:**
| File | Description |
|------|-------------|
| `data/model/SkyBrightnessResult.java` | Result model: Bortle class (1–9), label, description, computed luminance metrics |
| `ui/skybrightness/SkyBrightnessAnalyzer.java` | Image analysis engine: EXIF parsing + pixel luminance pipeline |
| `ui/skybrightness/SkyBrightnessActivity.java` | Full-screen sky brightness activity with image picker, analysis, and result display |
| `ui/skybrightness/BortleScaleView.java` | Custom `View` drawing a Bortle 1–9 colour gauge with animated needle |

**New layouts:**
- `activity_sky_brightness.xml` — Full-screen layout with image preview, gauge, and result text
- `dialog_sky_brightness.xml` — Compact result dialog used from `PlateSolveActivity`

---

#### Planet Trajectory Enhancements
- **Trajectory lock-on**: long-pressing a planet now locks the trajectory overlay so it remains visible while the user pans or rotates the phone away from the planet; previously the trajectory disappeared as soon as the planet left the screen centre
- **Full orbit span**: trajectory now plots the complete orbital period for each planet rather than a fixed 60-day window (e.g., Mercury: ~88 days, Jupiter: ~4332 days shown at reduced density)
- **Unlock button**: appears on-screen while trajectory lock is active; tapping it exits lock mode and returns to normal sensor-tracking behaviour
- **Snap-to behaviour**: entering lock mode smoothly snaps the view to the locked planet before showing the trajectory

#### 3D Compass Enhancements
- Compass needle now tilts in **3D** based on device pitch (looking up/down), giving a more spatially accurate representation of orientation
- Pitch-based camera transform applied to the compass canvas for a realistic perspective effect
- Tilt is clamped to avoid visual artifacts at extreme angles (looking straight up or down)

#### Smooth Zenith Panning
- Panning behaviour near the zenith (looking straight up) is now smooth and singularity-free; previously the view could snap or rotate unpredictably when the pitch approached ±90°

---

### Changed

| File | Change |
|------|--------|
| `SkyCanvasView.java` | Added 3D orthonormal basis vectors for Rodrigues rotations; trajectory lock state and orbit-span calculation; smooth zenith panning logic |
| `SkyMapActivity.java` | Wired AstroBot FAB to open `ChatBottomSheetFragment`; passes selected object name and observer context to chat fragment |
| `CompassView.java` | 3D pitch/tilt rendering via canvas camera transforms |
| `OnboardingActivity.java` | Added sky quality page with Bortle scale introduction |
| `PlateSolveActivity.java` | Added `analyzeSkyBrightness()` background task; "Sky Quality" button revealed after image load |
| `SettingsActivity.java` | API key field with encrypted read/write; key validation feedback |
| `activity_sky_map.xml` | AstroBot FAB added |
| `activity_plate_solve.xml` | Sky Quality button added |
| `activity_settings.xml` | API key input section added |
| `AndroidManifest.xml` | Declared `SkyBrightnessActivity`; added `INTERNET` permission |
| `app/build.gradle` | Added `androidx.security:security-crypto:1.1.0` dependency |
| `strings.xml` | +56 new strings for chat UI, sky brightness, onboarding, and settings |
| `colors.xml` | Bortle class colour palette (9 entries, green → red scale) and chat UI semantic colours |
| `file_paths.xml` | Added `external-cache-path` for `captures/` subdirectory |

---

### Fixed (Code Review — PR #17)

Twelve issues identified by code review were resolved in this release:

| # | Severity | Fix |
|---|----------|-----|
| 1 | Major | Upgraded `security-crypto` from `1.1.0-alpha06` to stable `1.1.0` |
| 2 | Major | `OpenAIClient.sendMessage()` now throws `IOException` on HTTP errors instead of returning an error string as a normal `BotResponse`; callers can now properly distinguish errors and show a retry option |
| 3 | Critical | `IOException` from the non-streaming fallback inside `sendMessageStreaming()` is now caught and routed to `callback.onError()` instead of escaping the method silently |
| 4 | Critical | `response_format: json_object` is no longer set for streaming requests; previously each SSE delta token was a raw JSON fragment (e.g. `{"answer": "Hel`) which was rendered directly in the chat UI |
| 5 | Major | Planet ephemeris computation in `ChatBottomSheetFragment.appendVisiblePlanetChips()` moved to a background thread; static chips now appear immediately, planet chips are appended once the background computation completes |
| 6 | Critical | Data race on `ChatViewModel.messages` (an `ArrayList` mutated on the main thread and iterated on the executor thread) mitigated by taking an immutable snapshot on the calling thread before submitting work to the executor |
| 7 | Major | Hardcoded English error strings in `ChatViewModel` replaced with `R.string.chat_no_api_key` and `R.string.chat_error` from `strings.xml` |
| 8 | Major | `InputStream` leak in `PlateSolveActivity.analyzeSkyBrightness()` fixed with try-with-resources; stream is now closed even if `ExifInterface` constructor throws |
| 9 | Major | Two `InputStream` leaks in `SkyBrightnessActivity.loadScaledBitmap()` (first-pass bounds decode and second-pass full decode) fixed with try-with-resources |
| 10 | Major | `InputStream` leak in `SkyBrightnessActivity.loadExif()` fixed with try-with-resources |
| 11 | Major | `FileProvider` `external-cache-path` scoped from root `"/"` to `"captures/"` to limit exposure of the external cache directory |
| 12 | Major | Machine-specific `org.gradle.java.home` path removed from `gradle.properties`; the line is now commented out so each developer's environment uses its own JDK detection |

---

### New Resource Summary

| Type | Count | Description |
|------|-------|-------------|
| Java source files | 9 | 3 data classes, 6 UI classes |
| XML layouts | 6 | Chat, sky brightness, item views, dialog |
| Drawables | 4 | Chat bubbles, circle, bottom-sheet handle |
| String resources | 56 | Chat, sky brightness, onboarding, settings |
| Color resources | 19 | Bortle palette, chat semantic colours |

---

---

## [Week 6] — 2026-02-15

### Summary
First-run experience overhaul with an onboarding walkthrough and in-app tooltip tutorial, a new animated 3D compass widget, and six bug fixes addressing manual drag rendering, settings sync, tooltip rendering, panel overlap, tutorial state, and build configuration.

### Added
- **3D Rotating Compass** (`CompassView.java`) — animated compass widget on the sky map showing N/S/E/W relative to device azimuth
- **In-App Tooltip Tutorial** (`TooltipView.java`, `TooltipManager.java`, `TooltipConfig.java`) — 6-step first-launch walkthrough with dark scrim overlay and circular punch-through highlight; completion state persisted in SharedPreferences
- **Onboarding Walkthrough** (`OnboardingActivity.java`) — multi-page onboarding screen with star detection tips and feature overview for new users

### Fixed
- **Manual Drag Mode rendering** — `getViewAzimuth()`/`getViewAltitude()` now return manual values when manual mode is active; all 28 `projectToScreen()` calls use these methods so the sky actually moves when dragging (`SkyCanvasView.java`)
- **Manual Mode Settings Sync** — `SkyMapActivity.onResume()` re-reads the manual scroll preference from SharedPreferences to prevent stale ViewModel state persisting after toggling the setting (`SkyMapActivity.java`)
- **Tooltip highlight not rendering** — Added `setLayerType(LAYER_TYPE_SOFTWARE, null)` so `PorterDuff.Mode.CLEAR` correctly punches through the scrim (`TooltipView.java`)
- **Info panel overlap with FABs** — Changed info panel end constraint from `parent` to `fabSearch` so the close button no longer overlaps search and camera FABs (`activity_sky_map.xml`)
- **Tutorial marked complete prematurely** — `TooltipManager.start()` now returns early (without calling `finish()`) when the tooltip list is empty (`TooltipManager.java`)
- **Build system** — Replaced hardcoded Java path with robust cross-platform JDK detection (`gradle.properties`)

---

## [Week 5] — 2026-02-10

### Summary
Sky map UX overhaul introducing pinch-to-zoom, planet trajectory visualisation, the full Messier deep sky object catalog, a Tonight's Highlights panel, and enhanced educational content.

### Added
- **Pinch-to-Zoom** — adjustable field of view from 20° to 120° via standard pinch gesture (`SkyCanvasView.java`)
- **Planet Trajectory Visualisation** — long-press any planet to display its projected 60-day orbital path with date/time labels
- **Deep Sky Objects — Messier Catalog** — galaxies (diamonds), open/globular clusters (squares), and nebulae (glowing circles) rendered on the sky map with a toggle in the bottom control bar
- **Tonight's Highlights Panel** — one-tap summary of all objects visible above the horizon right now; tap any item to navigate the sky map to that object
- **Smart Selection UI** — reticle-based tap selection with a compact chip strip (2–4 nearby objects) or an expandable bottom sheet (5+ objects)
- **Enhanced Star Education** — detailed information cards for the 100 brightest stars including spectral type, distance, and mythology notes
- **Manual Pan Mode** — drag to override sensor tracking; double-tap to reset; enable via Settings

---

## [Week 4] — 2026-02-03

### Summary
Native plate solving integration: astrometry.net ported via JNI, enabling on-device star detection (simplexy) and WCS plate solving matching `solve-field` output.

### Added
- **JNI Plate Solver** — `libastrometry_native.so` compiled for `arm64-v8a` and `armeabi-v7a`; runs simplexy star detection and the astrometry.net quad-matching solver on-device
- **Star Detection** — `detectStarsNative()` returns star positions matching `solve-field --downsample 2` output (677 ± 50 stars on reference image)
- **Plate Solving** — `solveFieldNative()` returns RA/Dec within 0.5° of reference (test result: RA = 81.37°, Dec = −0.99°)
- **Resort + Uniformize reordering** — implemented in JNI to replicate `solve-field`'s three-step star ordering (resort interleave + spatial grid uniformize + cut), ensuring field-spanning quads are formed at solve depth 21–30
- `PlateSolveActivity` — UI for loading a photo, running detection, and displaying the solved coordinates
- Index files 4115–4119 bundled in Android assets

### Fixed
- Removed erroneous `starxy_sort_by_flux()` call that pushed real stars below noise pixels
- `verify_pix` corrected from `2.0` to `1.0` (solve-field default)
- `tweak_aborder` / `tweak_abporder` corrected from `3` to `2` (solve-field default)
- `image2xy.c` assertion crash fixed when downsampling u8 images (`s->image_u8 = NULL` after upconvert)

---

## [Week 3] — 2026-01-27

### Summary
Constellation detection from camera images and richer star / constellation information.

### Added
- **Constellation Detection from Image** — detect star patterns in a photo and identify matching constellations
- **Constellation & Star Education** — updated information panels for all 88 constellations and 100 brightest stars
- **Chosen Star UI** — redesigned star detail bottom sheet with spectral class, distance, and constellation context

---

## [Week 2] — 2026-01-20

### Summary
Star search feature and navigation arrow guidance.

### Added
- **Star Search** — search bar for finding stars, planets, and constellations by name
- **Navigation Arrow** — directional arrow guides the user to the searched object; auto-dismisses when the object is centred on screen
- **Educational Content** — detailed display panels for constellations and planets

---

## [Week 1] — 2026-01-13

### Summary
Initial working app with real-time AR sky view.

### Added
- Real-time sky rendering using device sensors (accelerometer, magnetometer, gyroscope)
- 9,000+ stars from the Hipparcos catalogue with accurate positions and magnitudes
- 88 constellations with connecting lines and labels
- Sun, Moon, and all major planets with real-time orbital positions
- Night mode (red theme for dark adaptation)
- Alt/Az coordinate grid overlay
- GPS-based location for accurate sky alignment
- Time Travel — view the sky at any historical or future date/time
- Star tap for name, magnitude, and constellation information
- Camera preview with sensor-driven AR overlay

---

*For detailed technical notes on the astrometry.net JNI integration see [CLAUDE.md](CLAUDE.md).*
