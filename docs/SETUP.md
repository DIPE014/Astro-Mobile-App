# Setup Guide

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK 34
- Physical Android device (recommended for camera/sensors)

## Quick Start

1. **Clone and open**
   ```bash
   git clone <repo-url>
   cd Astro-Mobile-App
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Run on device**
   - Connect Android device via USB
   - Enable USB debugging on device
   - Click Run (green play button)

## Permissions

The app will request:
- **Camera** - For AR overlay mode
- **Location** - For accurate star positions

## Troubleshooting

### Build fails with protobuf error
Make sure you have the latest Gradle sync. The project uses protobuf 3.25.5.

### Stars don't match real sky
- Ensure GPS is enabled and location permission granted
- Wait a few seconds for GPS lock
- Check that device sensors are calibrated (figure-8 motion)

### App crashes on launch
- Ensure minSdk 26 (Android 8.0) or higher
- Check that all permissions are granted
