@echo off
REM Helper script to build with Java 17
REM This ensures the build uses the correct Java version regardless of system default

set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
echo Using Java from: %JAVA_HOME%

call gradlew.bat %*
