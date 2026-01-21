# Setup Guide

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Physical Android device (recommended for sensors/camera)

## Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd Astro-Mobile-App
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder

3. **Sync Gradle**
   - Android Studio will prompt to sync

4. **Copy star data from stardroid**
   ```bash
   cp ../stardroid/app/src/main/assets/stars.binary app/src/main/assets/
   cp ../stardroid/app/src/main/assets/constellations.binary app/src/main/assets/
   ```

5. **Run on device**
   - Connect Android device
   - Click Run (green play button)

## Permissions Required
- Camera
- Location (Fine + Coarse)
- Sensors (automatic)
