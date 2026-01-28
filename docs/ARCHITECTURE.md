# Architecture

## Overview

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│   SkyMapActivity, SearchActivity, etc.  │
├─────────────────────────────────────────┤
│            Core Layer                   │
│  AstronomerModel, Sensors, Renderer     │
├─────────────────────────────────────────┤
│            Data Layer                   │
│   StarRepository, ConstellationRepo     │
└─────────────────────────────────────────┘
```

## Data Flow

```
┌──────────┐     ┌───────────────────┐     ┌──────────────┐
│  Sensors │────▶│  AstronomerModel  │────▶│ SkyCanvasView│
└──────────┘     └───────────────────┘     └──────────────┘
      │                   │                       │
      ▼                   ▼                       ▼
 Rotation            Celestial              Rendered
  Vector             Pointing                 Sky
                         │
      ┌──────────────────┼──────────────────┐
      ▼                  ▼                  ▼
┌──────────┐      ┌───────────┐      ┌──────────┐
│   GPS    │      │ Star Data │      │  Planets │
└──────────┘      └───────────┘      └──────────┘
```

## Key Components

### AstronomerModel
Transforms device sensor data into celestial coordinates using matrix math.

**Input**: Rotation vector from sensors, GPS location, current time
**Output**: Celestial pointing (RA/Dec of view center)

### SkyCanvasView
Renders the sky view using Android Canvas.

**Input**: Star data, planet positions, view orientation
**Output**: Rendered sky with stars, planets, constellations

### StarRepository
Loads star data from protobuf binary files.

**Source**: `stars.binary` (Hipparcos catalog)
**Output**: List of StarData objects

### PlanetsLayer
Calculates planet positions using orbital mechanics.

**Input**: Current time (or time-travel time)
**Output**: RA/Dec positions for each planet

## Coordinate Systems

| System | Description |
|--------|-------------|
| RA/Dec | Celestial coordinates (fixed to stars) |
| Alt/Az | Local coordinates (relative to horizon) |
| Screen | Pixel coordinates for rendering |

## Threading

- **UI Thread**: Touch events, view updates
- **Sensor Thread**: Orientation updates (60Hz)
- **Background**: Star data loading, search indexing
