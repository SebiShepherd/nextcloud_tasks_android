#!/bin/bash
# Clean all Gradle caches and build artifacts
# Use this when you have stubborn build issues that won't resolve with normal clean

set -e

echo "🧹 Cleaning all Gradle caches and build artifacts..."

# Stop Gradle daemon to release locks
echo "⏸️  Stopping Gradle daemon..."
./gradlew --stop || true

# Clean via Gradle
echo "🔧 Running Gradle clean..."
./gradlew clean || true

# Remove Gradle caches
echo "🗑️  Removing .gradle directory..."
rm -rf .gradle/

# Remove all build directories
echo "🗑️  Removing module build directories..."
rm -rf build/
rm -rf app/build/
rm -rf data/build/
rm -rf domain/build/

# Remove KAPT generated files
echo "🗑️  Removing KAPT generated files..."
rm -rf app/build/generated/
rm -rf data/build/generated/

# Optional: Clean Android Studio caches (uncomment if needed)
# echo "🗑️  Removing .idea caches..."
# rm -rf .idea/caches/
# rm -rf .idea/libraries/

echo ""
echo "✅ All caches cleaned!"
echo "📦 Run './gradlew installDebug' to rebuild"
