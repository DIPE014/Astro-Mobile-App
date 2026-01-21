# Architecture

## Overview

```
┌─────────────────────────────────────┐
│           UI Layer                  │  ← Frontend (Java + XML)
│   Activities, Fragments, Views      │
├─────────────────────────────────────┤
│         Common Layer                │  ← Shared models
│      StarData, Pointing, etc.       │
├─────────────────────────────────────┤
│          Core Layer                 │  ← Backend (Java)
│  AstronomerModel, Sensors, Render   │
├─────────────────────────────────────┤
│        Data & ML Layer              │  ← Backend (Java)
│   StarRepository, ConstellationML   │
└─────────────────────────────────────┘
```

## Packages

| Package | Owner | Purpose |
|---------|-------|---------|
| `ui/` | Frontend | Activities, Fragments, XML layouts |
| `common/` | Both | Shared data models and interfaces |
| `core/control/` | Backend | Astronomy calculations, sensors |
| `core/math/` | Backend | Vector math (from stardroid) |
| `core/renderer/` | Backend | OpenGL sky rendering |
| `data/` | Backend | Star data loading |
| `ml/` | Backend | Constellation recognition |

## Data Flow

1. **Sensors** → SensorController → device orientation
2. **GPS** → LocationController → user position
3. **AstronomerModel** combines both → sky pointing direction
4. **StarRepository** loads star data → visible stars list
5. **UI** renders stars on camera preview
