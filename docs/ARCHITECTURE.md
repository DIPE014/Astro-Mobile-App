# Architecture

## Overview

```
┌─────────────────────────────────────┐
│           UI Layer                  │  Activities, Fragments, Views
│   SkyMapActivity, SearchActivity    │
├─────────────────────────────────────┤
│         Common Layer                │  Shared models & interfaces
│   StarData, Pointing, GeocentricCoords
├─────────────────────────────────────┤
│          Core Layer                 │  Astronomy engine
│  AstronomerModel, Layers, Renderer  │
├─────────────────────────────────────┤
│          Data Layer                 │  Data access
│   StarRepository, ProtobufParser    │
└─────────────────────────────────────┘
```

## Key Packages

| Package | Purpose |
|---------|---------|
| `ui/skymap/` | Main AR sky view activity |
| `ui/search/` | Search activity and results |
| `ui/starinfo/` | Star detail view |
| `ui/settings/` | App settings |
| `core/control/` | AstronomerModel, sensors, location, time |
| `core/math/` | Vector3, Matrix3x3, coordinate math |
| `core/layers/` | Stars, planets, constellations, grid layers |
| `core/renderer/` | SkyCanvasView, OpenGL rendering |
| `data/` | Repositories, protobuf parsing |
| `search/` | Search index, arrow view |
| `common/model/` | Shared data models |

## Data Flow

```
┌──────────────┐    ┌─────────────────┐    ┌───────────────┐
│   Sensors    │───>│ AstronomerModel │───>│ SkyCanvasView │
│ (Rotation)   │    │ (Coordinate     │    │ (Rendering)   │
└──────────────┘    │  Transform)     │    └───────────────┘
                    └─────────────────┘
┌──────────────┐           │
│     GPS      │───────────┘
│ (Location)   │
└──────────────┘

┌──────────────┐    ┌─────────────────┐
│ Star Binary  │───>│ StarRepository  │───> Stars Layer
│   Files      │    │ (Data Loading)  │
└──────────────┘    └─────────────────┘
```

## Coordinate Transformation

The app converts between multiple coordinate systems:

1. **Phone Coordinates** - Raw sensor data (x, y, z in device frame)
2. **Local Coordinates** - North, East, Up at observer's location
3. **Celestial Coordinates** - Right Ascension / Declination (fixed to stars)
4. **Horizontal Coordinates** - Altitude / Azimuth (local sky position)
5. **Screen Coordinates** - Pixels on display

The `AstronomerModel` handles the critical phone→celestial transformation using matrix math.

## Dependency Injection

Uses Dagger 2 for dependency injection:

- `AppComponent` - Application-level dependencies
- `AppModule` - Provides singletons (repositories, controllers)
- Components injected into Activities via `AstroApplication`
