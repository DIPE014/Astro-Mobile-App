# Project Structure

## Overview

```
Astro-Mobile-App/
├── app/                        # Android application
│   ├── src/main/
│   │   ├── java/com/astro/app/ # Source code
│   │   ├── assets/             # Binary data files
│   │   ├── proto/              # Protocol buffer definitions
│   │   └── res/                # Android resources
│   └── src/test/               # Unit tests
├── docs/                       # Documentation
└── gradle/                     # Build system
```

---

## Source Code (`app/src/main/java/com/astro/app/`)

```
com/astro/app/
├── AstroApplication.java           # App entry point, Dagger setup
├── MainActivity.java               # Launch activity
│
├── ui/                             # User Interface
│   ├── skymap/
│   │   └── SkyMapActivity.java     # Main AR sky view
│   ├── starinfo/
│   │   └── StarInfoActivity.java   # Star detail screen
│   ├── search/
│   │   └── SearchActivity.java     # Search screen
│   ├── settings/
│   │   └── SettingsActivity.java   # App settings
│   └── timetravel/
│       └── TimeTravelDialogFragment.java
│
├── core/                           # Astronomy Engine
│   ├── control/
│   │   ├── AstronomerModel.java    # Coordinate transformation interface
│   │   ├── AstronomerModelImpl.java # Matrix-based transformation
│   │   ├── SensorController.java   # Device sensor handling
│   │   ├── LocationController.java # GPS handling
│   │   ├── TimeTravelClock.java    # Time manipulation
│   │   └── space/
│   │       ├── Universe.kt         # Solar system calculations
│   │       └── SolarSystemBody.kt  # Planet definitions
│   ├── math/
│   │   ├── Vector3.kt              # 3D vector operations
│   │   ├── Matrix3x3.kt            # 3x3 matrix operations
│   │   ├── RaDec.kt                # Right Ascension / Declination
│   │   ├── LatLong.kt              # Geographic coordinates
│   │   └── TimeUtils.kt            # Julian day, sidereal time
│   ├── layers/
│   │   ├── StarsLayer.java         # Star rendering data
│   │   ├── PlanetsLayer.java       # Planet rendering data
│   │   ├── ConstellationsLayer.java # Constellation lines
│   │   └── GridLayer.java          # Coordinate grid
│   └── renderer/
│       ├── SkyCanvasView.java      # Canvas-based sky renderer
│       ├── SkyRenderer.java        # OpenGL renderer
│       └── SkyGLSurfaceView.java   # GL surface view
│
├── data/                           # Data Layer
│   ├── model/
│   │   ├── StarData.java           # Star data model
│   │   ├── ConstellationData.java  # Constellation model
│   │   └── GeocentricCoords.java   # 3D celestial coordinates
│   ├── repository/
│   │   ├── StarRepository.java     # Star data interface
│   │   ├── StarRepositoryImpl.java # Star data implementation
│   │   └── ConstellationRepository.java
│   └── parser/
│       └── ProtobufParser.java     # Binary file parser
│
├── search/                         # Search System
│   ├── PrefixStore.java            # Trie for autocomplete
│   ├── SearchIndex.java            # Aggregates searchable objects
│   ├── SearchResult.java           # Search result model
│   └── SearchArrowView.java        # Directional arrow overlay
│
├── common/model/                   # Shared Models
│   └── Pointing.java               # View direction
│
└── di/                             # Dependency Injection
    ├── AppComponent.java           # Dagger component
    └── AppModule.java              # Dagger module
```

---

## Resources (`app/src/main/res/`)

```
res/
├── layout/                     # XML layouts
│   ├── activity_main.xml
│   ├── activity_sky_map.xml
│   ├── activity_star_info.xml
│   ├── activity_search.xml
│   ├── activity_settings.xml
│   └── dialog_time_travel.xml
├── values/
│   ├── colors.xml              # Color definitions
│   ├── strings.xml             # Text strings
│   ├── themes.xml              # App themes
│   └── dimens.xml              # Dimensions
└── drawable/                   # Icons, shapes
```

---

## Assets (`app/src/main/assets/`)

```
assets/
├── stars.binary                # Star catalog (~9000 stars)
├── constellations.binary       # 88 constellation definitions
└── messier.binary              # Deep sky objects (not used yet)
```

---

## Tests (`app/src/test/`)

251+ unit tests covering:
- Vector and matrix operations
- Coordinate transformations
- Time calculations
- Star data parsing
