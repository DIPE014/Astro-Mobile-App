# Setup Guide

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher (JDK 21 recommended — Android Studio bundles JBR 21)
- Android SDK 34
- Physical Android device (required for sensors/camera)

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/DIPE014/Astro-Mobile-App.git
   cd Astro-Mobile-App
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Run on device**
   - Connect an Android device via USB
   - Enable USB debugging on the device
   - Click Run (green play button)

## Build from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

## Permissions

The app requires these permissions (granted at runtime):

| Permission | Purpose |
|------------|---------|
| Camera | AR camera preview |
| Fine Location | Accurate sky positioning |
| Coarse Location | Fallback location |

Sensors (accelerometer, magnetometer, gyroscope) don't require explicit permission.

## Troubleshooting

**Stars not showing?**
- Make sure location permission is granted
- Wait a few seconds for data to load

**Orientation wrong?**
- Calibrate compass by moving phone in figure-8 pattern
- Make sure you're not near magnetic interference

**Build fails?**
- Sync Gradle files (File → Sync Project with Gradle Files)
- Check JDK version: AGP 8.3.2 requires JDK 17+. Use Android Studio's bundled JBR:
  ```bash
  # Windows (command line build)
  set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
  gradlew.bat assembleDebug

  # Linux/Mac
  export JAVA_HOME=/path/to/android-studio/jbr
  ./gradlew assembleDebug
  ```
