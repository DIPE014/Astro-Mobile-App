# Astro Mobile App

An Android augmented reality astronomy app that helps you explore the night sky. Point your phone at the sky and see stars, planets, and constellations overlaid on your camera view in real-time.

## 📢 Latest Release - Week 5 UX Update (February 10, 2026)

We've released a major update with enhanced sky map interactions and new features! 🎉

**New Features:**
- 🔍 **Pinch-to-Zoom** - Adjust field of view from 20° to 120°
- 🎯 **Planet Trajectories** - Long-press planets to see their 60-day orbital path
- ✨ **Deep Sky Objects** - Explore Messier catalog galaxies, clusters, and nebulae
- 🌟 **Tonight's Highlights** - Quick view of visible objects right now
- 📚 **Enhanced Education** - Detailed information for 100 brightest stars and all planets
- 🎨 **Smart Selection UI** - Improved object selection with chips and bottom sheets

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for full details, bug fixes, and known limitations.

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
