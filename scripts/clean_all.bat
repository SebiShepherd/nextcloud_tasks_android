@echo off
REM Clean all Gradle caches and build artifacts (Windows version)
REM Use this when you have stubborn build issues that won't resolve with normal clean

echo 🧹 Cleaning all Gradle caches and build artifacts...

REM Stop Gradle daemon to release locks
echo ⏸️  Stopping Gradle daemon...
call gradlew.bat --stop 2>nul

REM Clean via Gradle
echo 🔧 Running Gradle clean...
call gradlew.bat clean 2>nul

REM Remove Gradle caches
echo 🗑️  Removing .gradle directory...
if exist .gradle rmdir /s /q .gradle 2>nul

REM Remove all build directories
echo 🗑️  Removing module build directories...
if exist build rmdir /s /q build 2>nul
if exist app\build rmdir /s /q app\build 2>nul
if exist data\build rmdir /s /q data\build 2>nul
if exist domain\build rmdir /s /q domain\build 2>nul

REM Remove KAPT generated files
echo 🗑️  Removing KAPT generated files...
if exist app\build\generated rmdir /s /q app\build\generated 2>nul
if exist data\build\generated rmdir /s /q data\build\generated 2>nul

echo.
echo ✅ All caches cleaned!
echo 📦 Run 'gradlew.bat installDebug' to rebuild
pause
