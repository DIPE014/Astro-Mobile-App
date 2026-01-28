# Project Structure

## Root Directory

```
Astro-Mobile-App/
├── app/                    # Android application module
├── docs/                   # Documentation
├── gradle/                 # Gradle wrapper
├── build.gradle            # Root build config
├── settings.gradle         # Module settings
└── README.md               # User-facing readme
```

## App Module (`app/src/main/`)

### Java Source (`java/com/astro/app/`)

```
com/astro/app/
├── AstroApplication.java       # App entry point, Dagger setup
├── MainActivity.java           # Launch screen
│
├── ui/                         # UI Layer
│   ├── skymap/                 # Main AR sky view
│   │   ├── SkyMapActivity.java
│   │   ├── SkyMapViewModel.java
│   │   ├── CameraManager.java
│   │   └── AROverlayManager.java
│   ├── search/                 # Search screen
│   │   ├── SearchActivity.java
│   │   └── SearchResultAdapter.java
│   ├── starinfo/               # Star detail screen
│   │   ├── StarInfoActivity.java
│   │   └── StarInfoViewModel.java
│   ├── settings/               # Settings screen
│   │   ├── SettingsActivity.java
│   │   └── SettingsViewModel.java
│   ├── timetravel/             # Time travel dialog
│   │   └── TimeTravelDialogFragment.java
│   └── common/                 # Shared UI components
│       ├── ErrorDialog.java
│       └── LoadingDialog.java
│
├── core/                       # Core Engine
│   ├── control/                # Controllers
│   │   ├── AstronomerModel.java
│   │   ├── AstronomerModelImpl.java
│   │   ├── SensorController.java
│   │   ├── LocationController.java
│   │   ├── TimeTravelClock.java
│   │   └── space/              # Solar system
│   │       ├── Universe.kt
│   │       ├── SolarSystemObject.kt
│   │       └── Moon.kt
│   ├── math/                   # Math utilities
│   │   ├── Vector3.kt
│   │   ├── Matrix3x3.kt
│   │   ├── RaDec.kt
│   │   ├── LatLong.kt
│   │   ├── TimeUtils.kt
│   │   └── CoordinateManipulations.kt
│   ├── layers/                 # Rendering layers
│   │   ├── Layer.java
│   │   ├── AbstractLayer.java
│   │   ├── StarsLayer.java
│   │   ├── PlanetsLayer.java
│   │   ├── ConstellationsLayer.java
│   │   └── GridLayer.java
│   └── renderer/               # Sky rendering
│       ├── SkyCanvasView.java
│       ├── SkyGLSurfaceView.java
│       ├── SkyRenderer.java
│       ├── PointRenderer.java
│       ├── LineRenderer.java
│       └── LabelRenderer.java
│
├── data/                       # Data Layer
│   ├── model/                  # Data models
│   │   ├── StarData.java
│   │   ├── ConstellationData.java
│   │   └── GeocentricCoords.java
│   ├── repository/             # Data access
│   │   ├── StarRepository.java
│   │   ├── StarRepositoryImpl.java
│   │   ├── ConstellationRepository.java
│   │   └── ConstellationRepositoryImpl.java
│   └── parser/                 # Data parsing
│       ├── ProtobufParser.java
│       └── AssetDataSource.java
│
├── search/                     # Search Engine
│   ├── SearchIndex.java
│   ├── SearchResult.java
│   ├── PrefixStore.java
│   └── SearchArrowView.java
│
├── common/                     # Shared
│   └── model/
│       └── Pointing.java
│
└── di/                         # Dependency Injection
    ├── AppComponent.java
    └── AppModule.java
```

### Resources (`res/`)

```
res/
├── layout/                 # XML layouts
├── values/                 # Colors, strings, themes
└── drawable/               # Images and icons
```

### Assets (`assets/`)

```
assets/
├── stars.binary            # Hipparcos star catalog
├── constellations.binary   # Constellation data
└── messier.binary          # Deep sky objects
```

## Tests (`app/src/test/`)

```
test/java/com/astro/app/
├── core/math/              # Math tests
│   ├── Vector3Test.java
│   ├── Matrix3x3Test.java
│   ├── RaDecTest.java
│   └── TimeUtilsTest.java
└── data/model/             # Model tests
    ├── StarDataTest.java
    └── GeocentricCoordsTest.java
```
