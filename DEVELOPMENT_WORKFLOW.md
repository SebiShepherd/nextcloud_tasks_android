# Entwicklungs-Workflow für Nextcloud Tasks Android

## Schnelle Referenz: Wann welchen Build-Befehl verwenden?

### ✅ Empfohlene Methoden (von schnell nach sicher)

| Situation | Befehl | Dauer | Zuverlässigkeit |
|-----------|--------|-------|-----------------|
| **Kleine Code-Änderung nur in :app** | `./gradlew installDebug` | ~10-30s | ⭐⭐⭐⭐ |
| **Änderung in :data oder :domain** | `./gradlew installDebug` | ~20-40s | ⭐⭐⭐⭐ |
| **Nach git pull / Dependency-Updates** | `./gradlew clean installDebug` | ~60-90s | ⭐⭐⭐⭐⭐ |
| **Hartnäckige Cache-Probleme** | Siehe unten: "Nuclear Option" | ~90-120s | ⭐⭐⭐⭐⭐ |

### ❌ NICHT empfohlen

| Android Studio Button | Problem |
|----------------------|---------|
| "Apply Changes" | Funktioniert fast nie bei Multi-Modul-Projekten |
| "Apply Code Changes" | Ignoriert Ressourcen-Änderungen, Hilt-Updates |
| Grüner "Run"-Button | Oft inkrementelles Build, überspringt manchmal Module |

---

## Typische Workflows

### 1. Normale Entwicklung (Quick Iteration)

```bash
# Nach Code-Änderungen
./gradlew installDebug

# Oder mit automatischem Start der App
adb shell am start -n com.nextcloud.tasks.debug/.MainActivity
```

**Keyboard-Shortcut in Android Studio:**
- Öffne `Run > Edit Configurations...`
- Erstelle neue "Gradle" Configuration
- Tasks: `installDebug`
- Weise Shortcut zu: `Ctrl+Shift+D` (oder eigene Wahl)

### 2. Nach Git Pull

```bash
# Immer nach git pull von Remote
./gradlew clean installDebug
```

**Warum?** Gradle erkennt nicht immer Änderungen in anderen Branches/Commits.

### 3. Nach Dependency-Updates

```bash
# Nach Änderungen in libs.versions.toml
./gradlew clean build --refresh-dependencies
./gradlew installDebug
```

### 4. "Nuclear Option" (bei hartnäckigen Problemen)

```bash
# Kompletter Reset des Build-Caches
./gradlew clean
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
rm -rf data/build/
rm -rf domain/build/
./gradlew installDebug
```

**Oder kürzer:**
```bash
# Bash-Script dafür
./scripts/clean_all.sh  # (falls vorhanden, sonst erstellen)
```

---

## Warum "Apply Changes" nicht funktioniert

### Multi-Modul-Struktur
```
:app (UI)
  ↓ depends on
:data (Repository, Database)
  ↓ depends on
:domain (Models, Use Cases)
```

**Problem:** Android Studio's "Apply Changes" versteht diese Abhängigkeiten nicht richtig.

### Hilt Dependency Injection
- DI-Graph wird zur **Compile-Time** generiert
- "Apply Changes" überspringt Kapt/KSP-Generierung
- Resultat: Alte DI-Bindings bleiben aktiv

### Ressourcen & Manifest
- `strings.xml`, `colors.xml` → Erfordern vollständiges Re-Packaging
- `AndroidManifest.xml` → Erfordert APK-Neuinstallation

---

## Performance-Optimierungen (bereits aktiviert)

### gradle.properties
```properties
# Build Cache (wichtig!)
org.gradle.caching=true

# Parallele Module-Builds
org.gradle.parallel=true

# Nur betroffene Module konfigurieren
org.gradle.configureondemand=true

# Inkrementelle Kotlin-Kompilierung
kotlin.incremental=true
kotlin.compiler.execution.strategy=in-process

# Mehr RAM für Gradle
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### Erwartete Build-Zeiten
- **Incremental Build** (nur :app geändert): 10-20s
- **Multi-Modul Build** (:data geändert): 30-40s
- **Clean Build**: 60-90s

---

## Debugging bei Build-Problemen

### 1. Prüfe, welche Tasks laufen
```bash
# Verbose Output
./gradlew installDebug --info

# Oder mit Scan (detaillierter)
./gradlew installDebug --scan
```

### 2. Prüfe, ob Module neu kompiliert werden
```bash
# Module explizit bauen
./gradlew :domain:build :data:build :app:assembleDebug
```

### 3. Cache-Status prüfen
```bash
# Gradle-Cache leeren (wenn aktiviert)
./gradlew cleanBuildCache
```

### 4. Gradle Daemon neustarten
```bash
# Bei seltsamen Problemen
./gradlew --stop
./gradlew installDebug
```

---

## Android Studio Konfiguration

### Disable "Instant Run" (veraltet, sollte deaktiviert sein)
- **File > Settings > Build, Execution, Deployment > Debugger**
- Deaktiviere alle "Apply Changes"-Features wenn Probleme auftreten

### Gradle Settings optimieren
- **File > Settings > Build, Execution, Deployment > Compiler**
  - ✅ `Build project automatically` (nur wenn gewünscht)
  - ✅ `Compile independent modules in parallel`
  - ✅ `Configure on demand`

- **File > Settings > Build, Execution, Deployment > Gradle**
  - Gradle JDK: **17 (Temurin/OpenJDK)**
  - Build and run using: **Gradle** (nicht Android Studio)
  - Run tests using: **Gradle** (nicht Android Studio)

---

## Häufige Fehlermeldungen

### "Task ':app:installDebug' uses this output of task ':data:compileDebugKotlin' without declaring an explicit or implicit dependency"

**Lösung:**
```bash
./gradlew clean build
```

### "Execution failed for task ':app:kaptGenerateStubsDebugKotlin'"

**Lösung:** Hilt-Cache-Problem
```bash
rm -rf app/build/generated/
./gradlew installDebug
```

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Lösung:** App-ID-Konflikt (Debug vs Release)
```bash
# Deinstalliere alte Version
adb uninstall com.nextcloud.tasks.debug
./gradlew installDebug
```

---

## Testing Workflow

### Unit Tests (schnell, lokal)
```bash
# Alle Module
./gradlew testDebugUnitTest

# Spezifisches Modul
./gradlew :domain:test
./gradlew :data:testDebugUnitTest
```

### Lint & Code Quality
```bash
# Vor jedem Commit
./gradlew ktlintCheck detekt :app:lintDebug

# Automatische Formatierung
./gradlew ktlintFormat
```

---

## Empfohlener Tagesablauf

### Morgens (nach git pull)
```bash
git pull origin main
./gradlew clean installDebug
```

### Während Entwicklung (iterativ)
```bash
# Nach jeder Änderung
./gradlew installDebug
```

### Vor Commit
```bash
# Code Quality Checks
./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest

# Falls Fehler:
./gradlew ktlintFormat  # Auto-fix
```

### Abends (optional: Cache aufräumen)
```bash
# Gradle Daemon stoppen (spart RAM)
./gradlew --stop
```

---

## Nützliche Alias/Scripts

Erstelle in `~/.bashrc` oder `~/.zshrc`:

```bash
# Android Development
alias gid="./gradlew installDebug"
alias gcid="./gradlew clean installDebug"
alias glint="./gradlew ktlintCheck detekt :app:lintDebug"
alias gtest="./gradlew testDebugUnitTest"
alias gcheck="./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest"
```

Dann kannst du einfach tippen:
```bash
gid      # Schneller Build
gcid     # Clean Build
gcheck   # Alle Checks
```

---

## Zusammenfassung

### ✅ DO:
- **Benutze Gradle-Tasks direkt** statt Android Studio-Buttons
- **Clean Build nach git pull**
- **Gradle Caching aktivieren** (✓ bereits erledigt)
- **Gradle Daemon nutzen** (automatisch nach erstem Build)

### ❌ DON'T:
- Android Studio "Apply Changes" bei Multi-Modul-Projekten
- Builds ohne vorheriges Clean nach großen Änderungen
- Gradle Cache deaktivieren (war vorher aus, jetzt an)

### 🚀 Quick Win:
Ab jetzt: **`./gradlew installDebug`** statt Android Studio-Buttons!

---

**Pro-Tipp:** Erstelle in Android Studio eine "Run Configuration" mit diesem Gradle-Task und weise ihr `Ctrl+Shift+R` oder ähnliches zu. Dann hast du einen zuverlässigen "Run"-Button!
