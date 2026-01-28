# Developer Documentation

Technical documentation for Astro Mobile App development.

## Contents

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | High-level system architecture |
| [STRUCTURE.md](./STRUCTURE.md) | Project folder structure |
| [CORE_CONCEPTS.md](./CORE_CONCEPTS.md) | Astronomy concepts and coordinate systems |
| [SETUP.md](./SETUP.md) | Development environment setup |

## Quick Start

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### Key Directories

```
app/src/main/java/com/astro/app/
├── ui/                 # Activities & UI components
├── core/
│   ├── control/        # AstronomerModel, sensors, location
│   ├── math/           # Vector3, Matrix3x3, coordinates
│   ├── layers/         # Stars, planets, constellations layers
│   └── renderer/       # SkyCanvasView, OpenGL rendering
├── data/               # Repositories, parsers
├── search/             # Search functionality
└── common/             # Shared models
```

## Key Components

### Coordinate Transformation Flow

```
Sensor Data → AstronomerModel → Celestial Pointing (RA/Dec) → Alt/Az → Screen Position
```

The app uses matrix-based sensor transformation (matching stardroid's approach):
1. Rotation vector from sensors
2. Extract [North, East, Up] vectors from rotation matrix
3. Build transformation matrix to celestial coordinates
4. Get pointing direction in celestial frame (RA/Dec)
5. Convert to local Alt/Az for rendering

### Main Files by Feature

| Feature | Files |
|---------|-------|
| Sky View | `SkyCanvasView.java`, `SkyMapActivity.java` |
| Sensors | `AstronomerModelImpl.java`, `SensorController.java` |
| Star Data | `StarRepositoryImpl.java`, `ProtobufParser.java` |
| Planets | `PlanetsLayer.java`, `Universe.kt`, `SolarSystemBody.kt` |
| Search | `SearchActivity.java`, `SearchArrowView.java` |
| Time Travel | `TimeTravelClock.java`, `TimeTravelDialogFragment.java` |

## Testing

251+ unit tests covering:
- Math utilities (Vector3, Matrix3x3, RaDec)
- Time calculations (LST, Julian Date)
- Data models (StarData, GeocentricCoords)

## Reference

Astronomy calculations adapted from [stardroid](https://github.com/sky-map-team/stardroid) (Apache 2.0).
