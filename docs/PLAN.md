# Implementation Plan

## Overview

Build an Android astronomy AR app by adapting core features from `stardroid` (Sky Map).

**Note**: AI/ML constellation recognition is excluded from this phase. Focus on GPS-based star positioning first.

---

## Tech Stack

- **Language**: Java (primary), Kotlin where beneficial
- **UI**: XML Layouts with Material Design 3
- **DI**: Dagger 2
- **Camera**: CameraX for AR preview
- **Data**: Protocol Buffers (reuse stardroid's star catalogs)
- **Min SDK**: 26 (Android 8.0)

---

## Phase 1: Project Setup

### Tasks
- [ ] Configure build.gradle with all dependencies
- [ ] Set up Dagger 2 dependency injection
- [ ] Copy math utilities from stardroid
- [ ] Copy binary data files (stars.binary, constellations.binary, messier.binary)
- [ ] Copy and configure protocol buffer definition
- [ ] Create AndroidManifest with required permissions
- [ ] Verify project compiles and runs

### Files to Copy from Stardroid

**Math Utilities** (copy to `core/math/`):
```
stardroid/app/src/main/java/com/google/android/stardroid/math/
├── Vector3.kt
├── Matrix3x3.kt
├── Matrix4x4.kt
├── RaDec.kt
├── LatLong.kt
├── Astronomy.kt
├── CoordinateManipulations.kt
├── Geometry.kt
└── MathUtils.kt
```

**Binary Data** (copy to `assets/`):
```
stardroid/app/src/main/assets/
├── stars.binary
├── constellations.binary
└── messier.binary
```

**Proto Definition** (copy to `datamodel/src/main/proto/`):
```
stardroid/datamodel/src/main/proto/source.proto
```

### Dependencies
```groovy
dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Dagger 2
    implementation 'com.google.dagger:dagger:2.50'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.50'

    // CameraX
    implementation 'androidx.camera:camera-camera2:1.3.1'
    implementation 'androidx.camera:camera-lifecycle:1.3.1'
    implementation 'androidx.camera:camera-view:1.3.1'

    // Location
    implementation 'com.google.android.gms:play-services-location:21.1.0'

    // Protocol Buffers
    implementation 'com.google.protobuf:protobuf-javalite:3.25.1'

    // Utilities
    implementation 'com.google.guava:guava:33.0.0-android'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## Phase 2: Data Layer (Database Role)

### Tasks
- [ ] Create StarData model class
- [ ] Create Constellation model class
- [ ] Create Pointing model class
- [ ] Implement ProtobufParser to read binary files
- [ ] Implement StarRepository
- [ ] Implement ConstellationRepository
- [ ] Add star search functionality
- [ ] Write unit tests for data layer

### Key Files
```
data/
├── model/
│   ├── StarData.java
│   ├── Constellation.java
│   └── CelestialObject.java
├── parser/
│   └── ProtobufParser.java
├── repository/
│   ├── StarRepository.java
│   └── ConstellationRepository.java
└── source/
    └── AssetDataSource.java
```

---

## Phase 3: Core Astronomy Engine (Backend Role)

### Tasks
- [ ] Adapt AstronomerModel from stardroid
- [ ] Implement LocationController with FusedLocationProvider
- [ ] Implement SensorController for device orientation
- [ ] Create coordinate transformation utilities
- [ ] Implement pointing calculation (device orientation → sky coordinates)
- [ ] Create Layer interface and base implementation
- [ ] Implement StarsLayer
- [ ] Implement ConstellationsLayer
- [ ] Write unit tests for core calculations

### Key Files
```
core/
├── control/
│   ├── AstronomerModel.java
│   ├── AstronomerModelImpl.java
│   ├── LocationController.java
│   └── SensorController.java
├── math/
│   └── (copied from stardroid)
├── layers/
│   ├── Layer.java
│   ├── AbstractLayer.java
│   ├── StarsLayer.java
│   └── ConstellationsLayer.java
└── util/
    └── TimeUtils.java
```

### Adapt from Stardroid
```
stardroid/app/.../control/AstronomerModelImpl.kt → Simplify, convert to Java
stardroid/app/.../control/LocationController.java → Use FusedLocationProvider
stardroid/app/.../control/SensorOrientationController.java → Simplify
stardroid/app/.../layers/StarsLayer.kt → Adapt
stardroid/app/.../layers/ConstellationsLayer.kt → Adapt
```

---

## Phase 4: Sky Renderer (Backend Role)

### Tasks
- [ ] Create SkyRenderer with OpenGL ES
- [ ] Implement star point rendering
- [ ] Implement constellation line rendering
- [ ] Implement text label rendering
- [ ] Add view matrix transformations
- [ ] Implement zoom and pan support
- [ ] Add night mode (red tint) rendering option
- [ ] Write rendering tests

### Key Files
```
core/renderer/
├── SkyRenderer.java
├── SkyGLSurfaceView.java
├── RendererController.java
├── primitive/
│   ├── PointPrimitive.java
│   ├── LinePrimitive.java
│   └── TextPrimitive.java
└── shader/
    ├── PointShader.java
    └── LineShader.java
```

---

## Phase 5: UI Layer (Frontend Role)

**IMPORTANT**: Build original, attractive UI. Do NOT copy stardroid UI.

### Design Goals
- Modern Material Design 3
- Dark theme optimized for night viewing
- Smooth animations and transitions
- Clean, minimalist interface
- Intuitive gesture controls

### Tasks
- [ ] Design app color scheme and typography
- [ ] Create MainActivity as navigation host
- [ ] Create SkyMapActivity with camera preview
- [ ] Create SkyMapFragment for sky rendering
- [ ] Implement SkyOverlayView custom view
- [ ] Create StarInfoActivity with attractive star details card
- [ ] Create SettingsActivity
- [ ] Implement smooth touch gestures (pan, zoom, tap)
- [ ] Create XML layouts with modern styling
- [ ] Implement night mode theme (red tint)
- [ ] Add loading animations
- [ ] Add transition animations between screens
- [ ] Add error handling with friendly UI

### Key Files
```
ui/
├── skymap/
│   ├── SkyMapActivity.java
│   ├── SkyMapFragment.java
│   ├── SkyOverlayView.java
│   └── SkyMapViewModel.java
├── starinfo/
│   ├── StarInfoActivity.java
│   └── StarInfoViewModel.java
├── settings/
│   └── SettingsActivity.java
└── common/
    ├── BaseActivity.java
    └── ViewUtils.java

res/layout/
├── activity_main.xml
├── activity_sky_map.xml
├── activity_star_info.xml
├── activity_settings.xml
├── fragment_sky_map.xml
└── item_star.xml

res/values/
├── colors.xml
├── strings.xml
├── themes.xml
├── themes_night.xml
└── dimens.xml
```

---

## Phase 6: Camera AR Integration

### Tasks
- [ ] Implement CameraManager with CameraX
- [ ] Set up camera preview in SkyMapActivity
- [ ] Overlay sky renderer on camera feed
- [ ] Calibrate camera FOV with sky coordinates
- [ ] Implement tap-to-select star on camera view
- [ ] Show star info popup on selection
- [ ] Handle camera permissions properly
- [ ] Add camera toggle (AR mode vs map mode)

### Key Files
```
ui/skymap/
├── CameraManager.java
└── CameraPermissionHandler.java
```

---

## Phase 7: Polish & Testing

### Tasks
- [ ] Add proper error handling throughout
- [ ] Implement loading indicators
- [ ] Add offline support verification
- [ ] Performance optimization
- [ ] Memory leak testing
- [ ] UI/UX refinements
- [ ] Code cleanup and documentation
- [ ] Final integration testing

---

## Shared Interfaces

All roles should implement/use these:

```java
// common/model/StarData.java
public class StarData {
    private String id;
    private String name;
    private float ra;           // Right Ascension
    private float dec;          // Declination
    private float magnitude;    // Brightness
    private int color;          // Display color
    // getters, setters, builder
}

// common/model/Pointing.java
public class Pointing {
    private float azimuth;      // Compass direction (0-360)
    private float altitude;     // Angle above horizon (-90 to 90)
    private float roll;         // Device roll
    // getters, setters
}

// common/model/Constellation.java
public class Constellation {
    private String id;
    private String name;
    private List<StarData> stars;
    private List<int[]> lineIndices;  // Which stars to connect
    // getters, setters
}
```

### Repository Interfaces
```java
public interface StarRepository {
    List<StarData> getAllStars();
    List<StarData> getVisibleStars(Pointing pointing, float fov);
    StarData getStarById(String id);
    List<StarData> searchByName(String query);
}

public interface ConstellationRepository {
    List<Constellation> getAllConstellations();
    Constellation getConstellationById(String id);
}
```

### Controller Interfaces
```java
public interface PointingProvider {
    Pointing getCurrentPointing();
    void addPointingListener(PointingListener listener);
    void removePointingListener(PointingListener listener);
}

public interface LocationProvider {
    LatLong getCurrentLocation();
    void addLocationListener(LocationListener listener);
}
```

---

## Work Assignment by Role

### Frontend
- Phase 5 (UI Layer)
- Phase 6 (Camera AR - UI parts)
- Phase 7 (UI polish)

### Backend
- Phase 1 (Project Setup - build config, DI)
- Phase 3 (Core Astronomy Engine)
- Phase 4 (Sky Renderer)

### Database
- Phase 1 (Copy data files, proto setup)
- Phase 2 (Data Layer)

### Shared
- `common/model/` classes
- Interface definitions
- Integration testing

---

## Testing Checkpoints

1. **After Phase 1**: App compiles, shows empty MainActivity
2. **After Phase 2**: Unit tests pass, can load star data from binary files
3. **After Phase 3**: Logs show correct RA/Dec when moving phone
4. **After Phase 4**: Stars render on GLSurfaceView
5. **After Phase 5**: Full UI navigation works
6. **After Phase 6**: Camera preview with stars overlaid
7. **After Phase 7**: All tests pass, app is polished

---

## File References from Stardroid

Key files to study/adapt:

| Purpose | Stardroid Path |
|---------|----------------|
| Coordinate math | `app/.../control/AstronomerModelImpl.kt` |
| Location | `app/.../control/LocationController.java` |
| Sensors | `app/.../control/SensorOrientationController.java` |
| Star rendering | `app/.../renderer/PointObjectManager.java` |
| Line rendering | `app/.../renderer/LineObjectManager.java` |
| Data loading | `app/.../layers/AbstractFileBasedLayer.kt` |
| Proto parsing | `app/.../source/ProtobufAstronomicalSource.kt` |
