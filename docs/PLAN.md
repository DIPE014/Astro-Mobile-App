# Implementation Plan

## Overview

Build an Android astronomy AR app by adapting core features from `stardroid` (Sky Map) and adding AI constellation recognition.

---

## Phase 1: Project Setup

### Tasks
- [ ] Set up Android project in Android Studio
- [ ] Configure build.gradle with dependencies
- [ ] Copy math utilities from stardroid (`stardroid/app/.../math/`)
- [ ] Copy binary data files (`stars.binary`, `constellations.binary`)
- [ ] Copy protocol buffer definition (`source.proto`)
- [ ] Create basic MainActivity

### Files to Copy from Stardroid
```
FROM: stardroid/app/src/main/java/com/google/android/stardroid/math/
  - Vector3.kt
  - Matrix3x3.kt
  - Matrix4x4.kt
  - RaDec.kt
  - LatLong.kt
  - Astronomy.kt
  - CoordinateManipulations.kt
  - Geometry.kt
TO: app/src/main/java/com/astro/app/core/math/

FROM: stardroid/app/src/main/assets/
  - stars.binary
  - constellations.binary
  - messier.binary
TO: app/src/main/assets/

FROM: stardroid/datamodel/src/main/proto/
  - source.proto
TO: datamodel/src/main/proto/
```

### Dependencies (build.gradle)
```groovy
dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'

    // Dagger 2
    implementation 'com.google.dagger:dagger:2.50'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.50'

    // CameraX
    implementation 'androidx.camera:camera-camera2:1.3.1'
    implementation 'androidx.camera:camera-lifecycle:1.3.1'
    implementation 'androidx.camera:camera-view:1.3.1'

    // Location
    implementation 'com.google.android.gms:play-services-location:21.1.0'

    // ML
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'

    // Protocol Buffers
    implementation 'com.google.protobuf:protobuf-javalite:3.25.1'

    // Utilities
    implementation 'com.google.guava:guava:33.0.0-android'
}
```

---

## Phase 2: Core Astronomy Engine (Backend)

### Tasks
- [ ] Adapt AstronomerModel from stardroid
- [ ] Implement LocationController (GPS)
- [ ] Implement SensorController (accelerometer, gyroscope, magnetometer)
- [ ] Create StarRepository to load binary data
- [ ] Calculate visible stars based on device pointing

### Key Files to Create
```
core/control/AstronomerModel.java    - Coordinate transformation
core/control/LocationController.java - GPS location
core/control/SensorController.java   - Device orientation
data/StarRepository.java             - Load star data
```

### Adapt from Stardroid
```
stardroid/app/.../control/AstronomerModelImpl.kt → Simplify, convert to Java
stardroid/app/.../control/LocationController.java → Modernize with FusedLocationProvider
stardroid/app/.../control/SensorOrientationController.java → Simplify
```

---

## Phase 3: Basic UI (Frontend)

### Tasks
- [ ] Create SkyMapActivity (main screen)
- [ ] Create XML layout with camera preview area
- [ ] Add basic Canvas overlay for star rendering
- [ ] Implement touch gestures (pan, zoom)
- [ ] Create StarInfoActivity for star details

### Key Files to Create
```
ui/skymap/SkyMapActivity.java
ui/skymap/SkyOverlayView.java        - Custom view for star overlay
ui/starinfo/StarInfoActivity.java
res/layout/activity_sky_map.xml
res/layout/activity_star_info.xml
```

---

## Phase 4: Camera AR Integration

### Tasks
- [ ] Add CameraX preview
- [ ] Overlay star rendering on camera feed
- [ ] Align camera FOV with sky coordinates
- [ ] Add tap-to-select star interaction

### Key Files to Create
```
ui/skymap/CameraManager.java         - CameraX setup
core/renderer/SkyRenderer.java       - OpenGL or Canvas rendering
```

---

## Phase 5: AI Constellation Recognition (Backend)

### Target Constellations (5-10)
1. Orion
2. Big Dipper (Ursa Major)
3. Cassiopeia
4. Scorpius
5. Leo
6. Cygnus
7. Southern Cross
8. Gemini

### Tasks
- [ ] Set up TensorFlow Lite
- [ ] Create ImageProcessor for camera frames
- [ ] Create ConstellationRecognizer class
- [ ] Obtain/train constellation classifier model
- [ ] Add UI toggle: GPS Mode / AI Mode

### Key Files to Create
```
ml/ConstellationRecognizer.java      - TFLite inference
ml/ImageProcessor.java               - Preprocess camera frames
assets/models/constellation.tflite   - ML model file
```

### ML Model Options
1. **Use pre-trained**: Search TensorFlow Hub for star/constellation models
2. **Train custom**: Use Stellarium screenshots as training data

---

## Phase 6: Polish

### Tasks
- [ ] Night mode (red theme for dark adaptation)
- [ ] Settings screen
- [ ] Error handling
- [ ] Performance optimization
- [ ] Testing

---

## Shared Interfaces (Define First)

Both frontend and backend should agree on these:

```java
// common/model/StarData.java
public class StarData {
    public String name;
    public float ra;           // Right Ascension (backend)
    public float dec;          // Declination (backend)
    public float screenX;      // Screen position (frontend uses)
    public float screenY;
    public float magnitude;    // Brightness
}

// common/model/Pointing.java
public class Pointing {
    public float azimuth;      // Compass direction
    public float altitude;     // Angle above horizon
}

// common/model/RecognizedConstellation.java
public class RecognizedConstellation {
    public String name;
    public float confidence;   // 0.0 to 1.0
}
```

---

## Work Assignment

### Frontend Person (Person A)
**Phase focus**: 3, 4 (UI parts), 6
**Folders**: `ui/`, `res/`

Week 1-2: Basic layouts, SkyMapActivity shell
Week 3-4: Camera preview, overlay view
Week 5-6: Touch interactions, StarInfoActivity
Week 7+: Polish, night mode, settings

### Backend Person (Person B)
**Phase focus**: 1, 2, 4 (renderer), 5
**Folders**: `core/`, `data/`, `ml/`

Week 1-2: Copy stardroid code, set up math utilities
Week 3-4: AstronomerModel, sensors, location
Week 5-6: Star data loading, coordinate calculations
Week 7+: ML constellation recognition

---

## Testing Checkpoints

1. **After Phase 1**: App compiles and runs
2. **After Phase 2**: Logs show correct RA/Dec when moving phone
3. **After Phase 3**: Stars appear on screen
4. **After Phase 4**: Camera preview with stars overlaid
5. **After Phase 5**: ML can identify Orion constellation
6. **Final**: Full demo with both GPS and AI modes
