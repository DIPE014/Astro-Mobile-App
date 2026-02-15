# Astro Mobile App

An Android augmented reality astronomy app that helps you explore the night sky. Point your phone at the sky and see stars, planets, and constellations overlaid on your camera view in real-time.

## 📢 Latest Release - Week 6: Onboarding, Compass & Bug Fixes (February 15, 2026)

This release focuses on first-time user experience, navigation polish, and critical bug fixes.

**New Features:**
- 🧭 **3D Rotating Compass** - A smooth, animated compass widget showing cardinal directions relative to your device orientation, always visible on the sky map
- 📖 **In-App Tooltip Tutorial** - Step-by-step walkthrough on first launch guiding users through constellation toggles, time travel, search, and star detection with highlighted anchor buttons and a scrim overlay
- 🎓 **Onboarding Walkthrough** - Multi-page onboarding screen with star detection tips and feature overview for new users

**Bug Fixes:**
- 🔧 **Manual Drag Mode** - Fixed rendering: all projection calls now respect manual azimuth/altitude so the sky actually moves when dragging
- 🔧 **Manual Mode Settings Sync** - Fixed stale ViewModel issue where toggling manual scroll OFF in Settings didn't take effect when returning to the sky map
- 🔧 **Tooltip Highlight** - Fixed PorterDuff.Mode.CLEAR not punching through the scrim (added software layer type)
- 🔧 **Info Panel Overlap** - Star info panel no longer overlaps with the search/camera FABs
- 🔧 **Tutorial Persistence** - Tutorial no longer marks itself completed when no tooltips are shown
- 🔧 **Build System** - Replaced hardcoded Java path with robust cross-platform detection

See [RELEASE_NOTES_WEEK6.md](RELEASE_NOTES_WEEK6.md) for full details.

---

### Week 5: Sky Map UX Update (February 10, 2026)

- Pinch-to-Zoom (20°-120° FOV), planet trajectory visualization, Deep Sky Objects (Messier catalog), Tonight's Highlights, enhanced education content for 100 brightest stars, smart selection UI with chips and bottom sheets.

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for Week 5 details.

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

## Credits

Core astronomy calculations adapted from [Sky Map (stardroid)](https://github.com/sky-map-team/stardroid) under Apache 2.0 License.

Star data from the Hipparcos catalog (ESA).

## License

This project is for educational purposes.

## Contributing

This is a school project. Feel free to fork and learn from it!

---

For developer documentation, see the [docs](./docs/) folder.
