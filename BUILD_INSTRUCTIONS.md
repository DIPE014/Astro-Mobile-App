# Build Instructions

## Requirements

- Android SDK (configured in `local.properties`)
- Java 11 or higher (Android Gradle Plugin 8.3.2 requirement)

## Building the Project

### Option 1: Using the Helper Script (Recommended for Windows)

If you encounter Java version issues on Windows:

```bash
build-with-java17.bat assembleDebug
```

This script automatically uses Gradle's cached Java 17 installation.

### Option 2: Standard Build

```bash
# Linux/Mac/WSL
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

### Option 3: Configure Java Manually

If automatic Java detection doesn't work, edit `gradle.properties` and uncomment the appropriate `org.gradle.java.home` line for your platform:

- **Windows (Android Studio JBR):**
  ```
  org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
  ```

- **Linux/WSL:**
  ```
  org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
  ```

- **macOS:**
  ```
  org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
  ```

## Common Issues

### "No matching variant of com.android.tools.build:gradle"

This error indicates Gradle is using Java 8, which is incompatible. Solutions:

1. Use `build-with-java17.bat` on Windows
2. Set `JAVA_HOME` environment variable to Java 11+
3. Configure `org.gradle.java.home` in `gradle.properties`

### NDK Errors

The project requires NDK version 25.1.8937393. Install it via Android Studio's SDK Manager.

## Output

Debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```
