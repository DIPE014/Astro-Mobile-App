# Astro-Mobile-App

An Android astronomy AR app that shows stars when you point your camera at the sky.

## Features

- **GPS + Time based positioning**: Uses device location and time to calculate visible stars
- **AR Camera Overlay**: Shows star names and constellations overlaid on camera view
- **AI Constellation Recognition**: Uses ML to identify constellations from camera (alternative to GPS)

## Tech Stack

- **Language**: Java + XML
- **Min SDK**: 26 (Android 8.0)
- **Key Libraries**: CameraX, TensorFlow Lite, Protocol Buffers, Dagger 2

## Project Structure

```
Astro-Mobile-App/
├── app/                          # Main Android application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/astro/app/
│   │   │   │   ├── ui/           # [FRONTEND] Activities, Fragments
│   │   │   │   │   ├── skymap/   # Main AR sky view
│   │   │   │   │   ├── starinfo/ # Star detail screen
│   │   │   │   │   └── settings/ # App settings
│   │   │   │   ├── core/         # [BACKEND] Astronomy engine
│   │   │   │   │   ├── control/  # AstronomerModel, sensors, location
│   │   │   │   │   ├── math/     # Vector3, Matrix, coordinates
│   │   │   │   │   ├── layers/   # Star/constellation data layers
│   │   │   │   │   └── renderer/ # OpenGL sky renderer
│   │   │   │   ├── data/         # [DATABASE] Repositories, star catalogs
│   │   │   │   ├── ml/           # [AI/ML] Constellation recognition
│   │   │   │   └── common/       # [SHARED] Models, interfaces
│   │   │   │       └── model/    # StarData, Pointing, etc.
│   │   │   ├── assets/           # Binary data files
│   │   │   │   └── models/       # TFLite ML models
│   │   │   └── res/              # [FRONTEND] Android resources
│   │   │       ├── layout/       # XML layouts
│   │   │       ├── values/       # Colors, strings, themes
│   │   │       └── drawable/     # Images, icons
│   │   └── test/                 # Unit tests
│   └── build.gradle
├── datamodel/                    # Protocol buffer definitions
│   └── src/main/proto/
├── docs/                         # Documentation
│   ├── PLAN.md                   # Implementation plan
│   └── STRUCTURE.md              # Detailed structure guide
├── gradle/
├── build.gradle                  # Root build file
├── settings.gradle
└── README.md                     # This file
```

## Work Division

| Role | Folders | Language |
|------|---------|----------|
| Frontend | `ui/`, `res/` | Java + XML |
| Backend | `core/` | Java |
| Database | `data/` | Java |
| AI/ML | `ml/` | Java |
| Shared | `common/model/` | Java |

## Getting Started

1. Open project in Android Studio
2. Sync Gradle
3. Copy star data files from `stardroid` project (see docs/PLAN.md)
4. Run on device with camera and GPS

## Adapted From

Core astronomy features adapted from [stardroid (Sky Map)](https://github.com/sky-map-team/stardroid) - Apache 2.0 License
