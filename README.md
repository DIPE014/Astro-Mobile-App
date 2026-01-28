# Astro - AR Night Sky Explorer

An Android augmented reality app that lets you explore the night sky. Point your phone at the sky and see stars, planets, and constellations overlaid on your camera view.

## Features

### Real-Time Star Map
- **9,000+ stars** rendered with accurate positions and colors
- Stars positioned using GPS location and device sensors
- Brightness-based sizing (brighter stars appear larger)

### Planets
- All major planets: Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune
- Sun and Moon tracking
- Positions calculated using real orbital mechanics (JPL ephemeris data)

### Constellation Lines
- 88 constellations with connecting lines
- Constellation names displayed at center
- Toggle on/off for cleaner viewing

### Search & Navigate
- Search for any star, planet, or constellation
- Directional arrow guides you to off-screen objects
- Auto-dismisses when target is centered

### Time Travel
- View the sky at any date/time in history or future
- Watch planets move through their orbits
- Quick presets: sunrise, sunset, lunar eclipse dates

### AR Camera Mode
- Overlay sky data on live camera feed
- Point your phone at the sky to identify objects
- Toggle between AR and map-only modes

### Additional Features
- **Night Mode** - Red-tinted display preserves dark adaptation
- **Coordinate Grid** - Alt/Az grid overlay for reference
- **Magnitude Control** - Filter stars by brightness
- **GPS Tracking** - Automatic location updates

## Screenshots

*Coming soon*

## Requirements

- Android 8.0 (API 26) or higher
- Camera permission (for AR mode)
- Location permission (for accurate star positions)
- Device with accelerometer and magnetometer sensors

## Installation

### From Source
1. Clone the repository
2. Open in Android Studio
3. Build and run on your device

```bash
git clone https://github.com/your-repo/Astro-Mobile-App.git
cd Astro-Mobile-App
```

### From APK
Download the latest release APK from the Releases page.

## How It Works

The app combines several data sources to show you the correct sky:

1. **Your Location** - GPS provides latitude/longitude
2. **Current Time** - Determines Earth's rotation and planet positions
3. **Device Orientation** - Accelerometer and magnetometer track where you're pointing
4. **Star Database** - 9,000+ stars with precise celestial coordinates (RA/Dec)
5. **Orbital Mechanics** - Calculates real-time positions of planets

All this data is transformed through coordinate math to render the correct portion of the sky on your screen.

## Tech Stack

- **Language**: Java + Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Camera**: CameraX
- **Rendering**: Custom Canvas renderer
- **Data**: Protocol Buffers for star catalog
- **DI**: Dagger 2

## Credits

Core astronomy calculations adapted from [Sky Map (stardroid)](https://github.com/sky-map-team/stardroid) under Apache 2.0 License.

Star catalog data from the Hipparcos mission.

## License

Apache 2.0 License - See [LICENSE](LICENSE) for details.
