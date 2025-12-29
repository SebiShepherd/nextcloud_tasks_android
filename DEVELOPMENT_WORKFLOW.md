# Development Workflow for Nextcloud Tasks Android

## Quick Reference: When to Use Which Build Command?

### ✅ Recommended Methods (from fast to safe)

| Situation | Command | Duration | Reliability |
|-----------|---------|----------|-------------|
| **Small code change in :app only** | `./gradlew installDebug` | ~10-30s | ⭐⭐⭐⭐ |
| **Change in :data or :domain** | `./gradlew installDebug` | ~20-40s | ⭐⭐⭐⭐ |
| **After git pull / dependency updates** | `./gradlew clean installDebug` | ~60-90s | ⭐⭐⭐⭐⭐ |
| **Stubborn cache issues** | See below: "Nuclear Option" | ~90-120s | ⭐⭐⭐⭐⭐ |

### ❌ NOT Recommended

| Android Studio Button | Problem |
|----------------------|---------|
| "Apply Changes" | Almost never works in multi-module projects |
| "Apply Code Changes" | Ignores resource changes, Hilt updates |
| Green "Run" button | Often incremental build, sometimes skips modules |

---

## Typical Workflows

### 1. Normal Development (Quick Iteration)

```bash
# After code changes
./gradlew installDebug

# Or with automatic app launch
adb shell am start -n com.nextcloud.tasks.debug/.MainActivity
```

**Keyboard Shortcut in Android Studio:**
- Open `Run > Edit Configurations...`
- Create new "Gradle" configuration
- Tasks: `installDebug`
- Assign shortcut: `Ctrl+Shift+D` (or your preference)

### 2. After Git Pull

```bash
# Always after git pull from remote
./gradlew clean installDebug
```

**Why?** Gradle doesn't always detect changes from other branches/commits.

### 3. After Dependency Updates

```bash
# After changes in libs.versions.toml
./gradlew clean build --refresh-dependencies
./gradlew installDebug
```

### 4. "Nuclear Option" (for stubborn problems)

```bash
# Complete reset of build caches
./gradlew clean
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
rm -rf data/build/
rm -rf domain/build/
./gradlew installDebug
```

**Or shorter:**
```bash
# Use the provided script
./scripts/clean_all.sh  # (if available, otherwise create it)
```

---

## Why "Apply Changes" Doesn't Work

### Multi-Module Structure
```
:app (UI)
  ↓ depends on
:data (Repository, Database)
  ↓ depends on
:domain (Models, Use Cases)
```

**Problem:** Android Studio's "Apply Changes" doesn't understand these dependencies correctly.

### Hilt Dependency Injection
- DI graph is generated at **compile-time**
- "Apply Changes" skips Kapt/KSP generation
- Result: Old DI bindings remain active

### Resources & Manifest
- `strings.xml`, `colors.xml` → Require complete re-packaging
- `AndroidManifest.xml` → Requires APK reinstallation

---

## Performance Optimizations (Already Enabled)

### gradle.properties
```properties
# Build Cache (important!)
org.gradle.caching=true

# Parallel module builds
org.gradle.parallel=true

# Configure only affected modules
org.gradle.configureondemand=true

# Incremental Kotlin compilation
kotlin.incremental=true
kotlin.compiler.execution.strategy=in-process

# More RAM for Gradle
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### Expected Build Times
- **Incremental Build** (only :app changed): 10-20s
- **Multi-Module Build** (:data changed): 30-40s
- **Clean Build**: 60-90s

---

## Debugging Build Problems

### 1. Check Which Tasks Are Running
```bash
# Verbose output
./gradlew installDebug --info

# Or with scan (more detailed)
./gradlew installDebug --scan
```

### 2. Check If Modules Are Recompiled
```bash
# Build modules explicitly
./gradlew :domain:build :data:build :app:assembleDebug
```

### 3. Check Cache Status
```bash
# Clear Gradle cache (if enabled)
./gradlew cleanBuildCache
```

### 4. Restart Gradle Daemon
```bash
# For weird issues
./gradlew --stop
./gradlew installDebug
```

---

## Android Studio Configuration

### Disable "Instant Run" (deprecated, should be disabled)
- **File > Settings > Build, Execution, Deployment > Debugger**
- Disable all "Apply Changes" features if problems occur

### Optimize Gradle Settings
- **File > Settings > Build, Execution, Deployment > Compiler**
  - ✅ `Build project automatically` (only if desired)
  - ✅ `Compile independent modules in parallel`
  - ✅ `Configure on demand`

- **File > Settings > Build, Execution, Deployment > Gradle**
  - Gradle JDK: **17 (Temurin/OpenJDK)**
  - Build and run using: **Gradle** (not Android Studio)
  - Run tests using: **Gradle** (not Android Studio)

---

## Common Error Messages

### "Task ':app:installDebug' uses this output of task ':data:compileDebugKotlin' without declaring an explicit or implicit dependency"

**Solution:**
```bash
./gradlew clean build
```

### "Execution failed for task ':app:kaptGenerateStubsDebugKotlin'"

**Solution:** Hilt cache problem
```bash
rm -rf app/build/generated/
./gradlew installDebug
```

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Solution:** App ID conflict (Debug vs Release)
```bash
# Uninstall old version
adb uninstall com.nextcloud.tasks.debug
./gradlew installDebug
```

---

## Testing Workflow

### Unit Tests (fast, local)
```bash
# All modules
./gradlew testDebugUnitTest

# Specific module
./gradlew :domain:test
./gradlew :data:testDebugUnitTest
```

### Lint & Code Quality
```bash
# Before each commit
./gradlew ktlintCheck detekt :app:lintDebug

# Automatic formatting
./gradlew ktlintFormat
```

---

## Recommended Daily Routine

### Morning (after git pull)
```bash
git pull origin main
./gradlew clean installDebug
```

### During Development (iterative)
```bash
# After each change
./gradlew installDebug
```

### Before Commit
```bash
# Code quality checks
./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest

# If errors occur:
./gradlew ktlintFormat  # Auto-fix
```

### Evening (optional: clean up cache)
```bash
# Stop Gradle daemon (saves RAM)
./gradlew --stop
```

---

## Useful Aliases/Scripts

Add to `~/.bashrc` or `~/.zshrc`:

```bash
# Android Development
alias gid="./gradlew installDebug"
alias gcid="./gradlew clean installDebug"
alias glint="./gradlew ktlintCheck detekt :app:lintDebug"
alias gtest="./gradlew testDebugUnitTest"
alias gcheck="./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest"
```

Then you can simply type:
```bash
gid      # Quick build
gcid     # Clean build
gcheck   # All checks
```

---

## Summary

### ✅ DO:
- **Use Gradle tasks directly** instead of Android Studio buttons
- **Clean build after git pull**
- **Enable Gradle caching** (✓ already done)
- **Use Gradle daemon** (automatic after first build)

### ❌ DON'T:
- Use Android Studio "Apply Changes" in multi-module projects
- Skip clean builds after major changes
- Disable Gradle cache (was off before, now on)

### 🚀 Quick Win:
From now on: **`./gradlew installDebug`** instead of Android Studio buttons!

---

**Pro Tip:** Create a "Run Configuration" in Android Studio with this Gradle task and assign it to `Ctrl+Shift+R` or similar. Then you have a reliable "Run" button!
