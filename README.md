# Astro Mobile App

An Android augmented reality astronomy app that helps you explore the night sky. Point your phone at the sky and see stars, planets, and constellations overlaid on your camera view in real-time.

## 📢 Latest Release — Week 8: UI Polish, Image Stacking & Splash Overhaul (March 12, 2026)

Major UI polish release with a new programmatic starfield splash, image stacking, radial FAB menu, and comprehensive onboarding.

**New Features:**
- ✨ **Programmatic Starfield Splash** — Replaced the blurry GIF intro with a custom `StarFieldView` rendering 220+ twinkling stars, shooting meteors (6–8 concurrent), and radial glow effects at 60 fps. Tap anywhere to trigger a 3-second warp tunnel transition (stars streak outward, shift bluer, white flash) before entering the sky map.
- 📷 **Image Stacking** — Combine multiple short-exposure sky photos to reveal fainter stars and nebulae. Native C pipeline using triangle asterism matching (rotation/scale/translation-invariant), RANSAC affine estimation, bilinear warp, and mean accumulation. Accessible via the new FAB menu.
- 🎯 **Radial FAB Menu** — MotionLayout-powered floating action button with four radial items: Search, Detect, Chat, and Stack. Overshoot animation with staggered reveal.
- 🗺️ **Constellation Boundary Resolver** — Tap anywhere on the sky to see which constellation that region belongs to, using IAU official boundary data.
- 📖 **Comprehensive Onboarding** — Expanded from 7 to 12 pages covering every feature. In-app tooltip tutorial expanded from 6 to 10 steps covering all controls.

**UI Improvements:**
- FAB menu repositioned higher (100dp margin) to avoid bottom navigation overlap
- "Tap anywhere to explore" hint with fade-in pulse animation on splash screen
- Exo font family (18 weights) for consistent typography
- Education detail screen redesigned with new section icons

See [CHANGELOG.md](CHANGELOG.md) for the full historical changelog.

---

### Week 7: AstroBot, Sky Brightness & 3D Rendering (February 23, 2026)

AstroBot AI assistant (ChatGPT-powered, context-aware), sky brightness meter (Bortle 1–9), planet trajectory lock-on, 3D compass tilt, smooth zenith panning, and 12 code-quality fixes.

### Week 6: Onboarding, Compass & Bug Fixes (February 15, 2026)

3D rotating compass, in-app tooltip tutorial (first-launch walkthrough), multi-page onboarding, and six bug fixes for manual drag rendering, settings sync, tooltip highlight, panel overlap, tutorial state, and build configuration.

### Week 5: Sky Map UX Update (February 10, 2026)

Pinch-to-Zoom (20°–120° FOV), planet trajectory visualisation, Deep Sky Objects (Messier catalog), Tonight's Highlights, enhanced education content for the 100 brightest stars, and a smart selection UI with chips and bottom sheets.

## Features

### Real-Time Sky View
Point your camera at the sky to see celestial objects rendered in augmented reality. The app uses your device's sensors (accelerometer, magnetometer, gyroscope) to accurately track where you're looking.

### Stars & Constellations
- **9,000+ stars** from the Hipparcos catalog with accurate positions
- **88 constellations** with connecting lines and labels
- Star names, magnitudes, and detailed information
- Filter stars by brightness (magnitude control)

### Planets & Solar System
- Sun, Moon, and all major planets (Mercury through Neptune)
- Real-time orbital calculations for accurate positioning
- Planet labels and visual indicators

### Search & Navigate
- Search for any star, planet, or constellation by name
- Navigation arrow guides you to your target
- Auto-dismisses when you center on the object

### Time Travel
- View the sky at any date and time in history or future
- See how the sky looked on your birthday
- Plan observations for upcoming celestial events

### Gestures & Interaction
- **Pinch-to-zoom** - Adjust field of view (20° to 120°)
- **Manual scroll mode** - Enable in Settings to drag-and-pan smoothly with reduced tap sensitivity (prevents accidental star info popups)
- **Manual pan** - Drag to override sensor tracking, double-tap to reset
- **Long-press planet** - View 60-day orbital trajectory with time labels
- **Smart selection** - Reticle-based selection with chips (2-4 objects) or bottom sheet (5+)

### Deep Sky Objects
- **Messier catalog** - Galaxies, star clusters, and nebulae rendered on the sky map
- Shape-coded icons: diamonds (galaxies), squares (clusters), glowing circles (nebulae)
- Toggle visibility from the bottom control bar

### Tonight's Highlights
- One-tap view of what's visible right now from your location
- Shows planets, bright stars, constellations, and deep sky objects above the horizon
- Tap any highlight to navigate the sky map to that object

### AstroBot AI Assistant
- Tap the chat FAB on the sky map to open AstroBot
- Ask anything: identify objects, plan observations, understand plate solving results, learn astronomy concepts
- Context-aware: the bot knows your GPS location, current time, what you are pointing at, and the selected object
- Suggestion chips adapt to which planets are currently above your horizon
- Follow-up questions suggested after every response
- Requires an OpenAI API key (enter in Settings; stored encrypted on-device)

### Sky Brightness Meter
- Load a sky photo in the Plate Solve screen and tap Sky Quality
- Estimates your Bortle dark-sky class (1 = pristine, 9 = inner-city) using EXIF exposure data and pixel luminance analysis
- Colour-coded Bortle scale gauge with class label and description

### Image Stacking
- Combine multiple short-exposure photos of the same sky region to reveal fainter objects
- Native C pipeline: triangle asterism matching (rotation/scale/translation-invariant alignment), RANSAC affine estimation, bilinear warp, and mean accumulation
- SNR improves by sqrt(N) for N frames — reveals stars and nebulosity invisible in single exposures
- Accessible via the Stack button in the FAB menu

### Additional Features
- **Night Mode** - Red theme to preserve dark adaptation
- **Coordinate Grid** - Alt/Az grid overlay for reference
- **GPS Tracking** - Automatic location updates for accurate sky positioning
- **Star Info** - Tap any star to see detailed information including parent constellation
- **Educational Content** - Detailed info for 100 brightest stars, all constellations, and planets

## Screenshots

*Coming soon*

## Requirements

- Android 8.0 (API 26) or higher
- Device with camera, GPS, and motion sensors
- Location and camera permissions

## Installation

### From Source
1. Clone the repository
2. Open in Android Studio
3. Build and run on your device

```bash
git clone https://github.com/DIPE014/Astro-Mobile-App.git
```

### APK Download
*Coming soon*

## How to Use

1. **Grant Permissions** - Allow camera and location access when prompted
2. **Point at the Sky** - Hold your phone up toward the sky
3. **Explore** - Move your phone around to discover stars and constellations
4. **Search** - Tap the search icon to find specific objects
5. **Toggle Layers** - Use the bottom bar to show/hide constellations, planets, grid, and deep sky objects
6. **Zoom** - Pinch to zoom in or out on the sky
7. **Manual Scroll** - Enable in Settings to drag-and-pan the sky map smoothly with your finger
8. **Trajectories** - Long-press a planet to see its path over the next 60 days
9. **Tonight's Sky** - Tap the calendar icon to see what's visible tonight

## Tech Stack

- **Language**: Java/Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Architecture**: MVVM with Dagger 2 dependency injection
- **Key Libraries**:
  - CameraX for camera preview
  - Protocol Buffers for star data
  - Google Play Services for location
  - androidx.security:security-crypto for encrypted API key storage
  - OpenAI Chat Completions API (GPT-5 Nano) for AstroBot

## Credits

Core astronomy calculations adapted from [Sky Map (stardroid)](https://github.com/sky-map-team/stardroid) under Apache 2.0 License.

Star data from the Hipparcos catalog (ESA).

## License

This project is for educational purposes.

## Contributing

This is a school project. Feel free to fork and learn from it!

---

For developer documentation, see the [docs](./docs/) folder.
