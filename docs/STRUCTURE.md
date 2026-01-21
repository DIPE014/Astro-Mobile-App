# Project Structure Guide

## Folder Overview

```
Astro-Mobile-App/
├── app/                    # Android application module
├── datamodel/              # Protocol buffer definitions
├── docs/                   # Documentation (you are here)
├── gradle/                 # Gradle wrapper
├── build.gradle            # Root build config
├── settings.gradle         # Module settings
└── README.md               # Project overview
```

---

## App Module Structure

### Source Code (`app/src/main/java/com/astro/app/`)

```
com/astro/app/
├── AstroApplication.java       # App entry point, Dagger setup
├── MainActivity.java           # Main activity
│
├── ui/                         # [FRONTEND - Person A]
│   ├── skymap/                 # Main AR sky view screen
│   │   ├── SkyMapActivity.java
│   │   ├── SkyMapFragment.java
│   │   └── SkyOverlayView.java
│   ├── starinfo/               # Star detail screen
│   │   └── StarInfoActivity.java
│   └── settings/               # Settings screen
│       └── SettingsActivity.java
│
├── core/                       # [BACKEND - Person B]
│   ├── control/                # Controllers
│   │   ├── AstronomerModel.java      # Core coordinate math
│   │   ├── LocationController.java   # GPS handling
│   │   └── SensorController.java     # Device sensors
│   ├── math/                   # Math utilities (from stardroid)
│   │   ├── Vector3.java
│   │   ├── Matrix3x3.java
│   │   ├── RaDec.java
│   │   └── ...
│   ├── layers/                 # Data layers
│   │   ├── Layer.java
│   │   ├── StarsLayer.java
│   │   └── ConstellationsLayer.java
│   └── renderer/               # Sky rendering
│       └── SkyRenderer.java
│
├── data/                       # [BACKEND - Person B]
│   └── StarRepository.java     # Load and manage star data
│
├── ml/                         # [BACKEND - Person B]
│   ├── ConstellationRecognizer.java  # ML inference
│   └── ImageProcessor.java           # Image preprocessing
│
├── di/                         # Dependency injection
│   ├── AppComponent.java
│   └── AppModule.java
│
└── common/                     # [SHARED - Both]
    └── model/                  # Data models
        ├── StarData.java
        ├── Pointing.java
        └── RecognizedConstellation.java
```

---

## Resources (`app/src/main/res/`)

```
res/
├── layout/                     # [FRONTEND - Person A]
│   ├── activity_main.xml
│   ├── activity_sky_map.xml
│   ├── activity_star_info.xml
│   └── fragment_sky_map.xml
│
├── values/                     # [FRONTEND - Person A]
│   ├── colors.xml              # Color definitions
│   ├── strings.xml             # Text strings
│   ├── themes.xml              # App themes
│   └── dimens.xml              # Dimensions
│
└── drawable/                   # [FRONTEND - Person A]
    └── (icons, images)
```

---

## Assets (`app/src/main/assets/`)

```
assets/
├── stars.binary                # Star catalog (from stardroid)
├── constellations.binary       # Constellation data (from stardroid)
├── messier.binary              # Deep sky objects (from stardroid)
└── models/
    └── constellation.tflite    # ML model (to be added)
```

---

## DataModel Module

```
datamodel/
└── src/main/proto/
    └── source.proto            # Protocol buffer schema (from stardroid)
```

---

## Language Reference

| Folder | Language | File Extension |
|--------|----------|----------------|
| `ui/` | Java | `.java` |
| `core/` | Java | `.java` |
| `data/` | Java | `.java` |
| `ml/` | Java | `.java` |
| `common/` | Java | `.java` |
| `res/layout/` | XML | `.xml` |
| `res/values/` | XML | `.xml` |
| `datamodel/` | Protocol Buffers | `.proto` |

---

## Who Works Where

### Frontend Person (Person A)
```
EDIT THESE:
├── ui/**/*.java           # All UI code
├── res/layout/*.xml       # All layouts
├── res/values/*.xml       # Colors, strings, themes
└── res/drawable/*         # Images, icons
```

### Backend Person (Person B)
```
EDIT THESE:
├── core/**/*.java         # Astronomy engine
├── data/**/*.java         # Data handling
├── ml/**/*.java           # ML code
└── assets/*               # Binary data files
```

### Both (Define Together)
```
├── common/model/*.java    # Shared data models
├── di/*.java              # Dependency injection
└── MainActivity.java      # May need coordination
```

---

## Key Interfaces Between Frontend & Backend

Frontend calls these (Backend implements):

```java
// Backend provides star data
public interface StarDataProvider {
    List<StarData> getVisibleStars();
    StarData getStarAtPosition(float screenX, float screenY);
}

// Backend provides device pointing
public interface PointingProvider {
    Pointing getCurrentPointing();
    void addPointingListener(PointingListener listener);
}

// Backend provides ML recognition
public interface ConstellationRecognizer {
    RecognizedConstellation recognize(Bitmap cameraFrame);
}
```

Frontend provides these (Backend uses):

```java
// Frontend tells backend about screen
public interface ScreenInfo {
    int getScreenWidth();
    int getScreenHeight();
    float getFieldOfView();
}
```
