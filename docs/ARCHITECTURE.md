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
| `core/highlights/` | Tonight's sky computation (TonightsHighlights) |
| `core/layers/` | Stars, planets, constellations, grid layers |
| `core/renderer/` | SkyCanvasView (zoom, pan, DSO, trajectory rendering) |
| `data/model/` | StarData, ConstellationData, MessierObjectData |
| `data/repository/` | Star, Constellation, Messier, Education repositories |
| `data/parser/` | Protobuf parsing |
| `ui/highlights/` | Tonight's Highlights bottom sheet |
| `ui/education/` | Educational detail views |
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

┌──────────────┐    ┌──────────────────┐
│ messier.bin  │───>│MessierRepository │───> DSO Layer
└──────────────┘    └──────────────────┘

┌──────────────┐    ┌──────────────────┐
│ JSON Assets  │───>│EducationRepo    │───> Education Detail
│ (education)  │    │(Boundary Resolve)│
└──────────────┘    └──────────────────┘
```

## User Interaction Flow

```
Pinch Gesture ──> ScaleGestureDetector ──> FOV change (20°-120°)
Drag Gesture  ──> GestureDetector ──────> Manual pan mode
Long-press    ──> Planet hit-test ──────> Trajectory overlay (±30 days)
Reticle Tap   ──> getObjectsInReticle() ─> Chip strip / Bottom sheet
Tonight Btn   ──> TonightsHighlights ───> Bottom sheet → Navigate
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

### Provided Dependencies (AppModule)

| Dependency | Implementation |
|-----------|----------------|
| `StarRepository` | `StarRepositoryImpl` (protobuf) |
| `ConstellationRepository` | `ConstellationRepositoryImpl` (protobuf) |
| `MessierRepository` | `MessierRepositoryImpl` (protobuf) |
| `Universe` | Solar system calculations |
| `TimeTravelClock` | Time control |
| `SensorController` | Device sensors |
| `LocationController` | GPS location |
| `AstronomerModel` | Coordinate transforms |
