# Release Guide für Nextcloud Tasks Android

Dieses Dokument beschreibt den kompletten Prozess für die Veröffentlichung von Releases der Nextcloud Tasks Android App.

---

## 📋 Übersicht

Die App verwendet einen automatisierten Release-Prozess basierend auf **Git Tags**. Sobald ein Version-Tag (z.B. `v1.0.0`) erstellt wird, baut GitHub Actions automatisch APK und AAB Dateien und erstellt ein GitHub Release mit Download-Links.

### Was wird automatisch gemacht:
✅ APK-Build (signiert, für direkten Download)
✅ AAB-Build (signiert, für Google Play Store)
✅ Quality Checks (ktlint, detekt, lint, tests)
✅ Automatische Versionierung
✅ GitHub Release mit Release Notes
✅ Download-URLs für APK und AAB

---

## 🔧 Einmalige Setup-Schritte in GitHub

### 1. Branch Protection für `main` einrichten

**Schritte:**
1. Gehe zu deinem Repository auf GitHub
2. Klicke auf **Settings** → **Branches** (links in der Sidebar)
3. Unter "Branch protection rules" klicke auf **Add rule**
4. Konfiguriere folgende Einstellungen:

   **Branch name pattern:**
   ```
   main
   ```

   **Protect matching branches - Aktiviere folgende Checkboxen:**

   - ✅ **Require a pull request before merging**
     - ✅ Require approvals: `1` (oder mehr)
     - ✅ Dismiss stale pull request approvals when new commits are pushed

   - ✅ **Require status checks to pass before merging**
     - ✅ Require branches to be up to date before merging
     - Wähle folgende Status Checks (erscheinen nach dem ersten CI-Run):
       - `quality` (von .github/workflows/ci.yml)

   - ✅ **Require conversation resolution before merging**

   - ✅ **Do not allow bypassing the above settings**
     - Optional: Erlaube Admins das Bypassen (für Notfälle)

   - ✅ **Restrict who can push to matching branches**
     - Optional: Nur bestimmte Personen/Teams erlauben

   - ✅ **Block force pushes** (sehr wichtig!)

   - ✅ **Require linear history** (empfohlen)

5. Klicke auf **Create** oder **Save changes**

**Wichtig:** Ab jetzt können nur noch PRs nach `main` gemerged werden, die:
- Von dir approved wurden
- Alle Quality Checks bestanden haben
- Keine offenen Diskussionen haben
- Keine Force-Pushes erlauben

---

### 2. Repository Secrets prüfen

Die folgenden Secrets müssen in deinem Repository hinterlegt sein (scheinen bereits vorhanden zu sein):

**Gehe zu:** Settings → Secrets and variables → Actions → Repository secrets

Erforderliche Secrets:
- `SIGNING_KEYSTORE_BASE64` - Base64-kodiertes Android Keystore
- `SIGNING_KEYSTORE_PASSWORD` - Passwort für den Keystore
- `SIGNING_KEY_ALIAS` - Alias des Signing Keys
- `SIGNING_KEY_PASSWORD` - Passwort für den Signing Key

**Diese Secrets sind bereits konfiguriert ✅**

Optional (für Play Store Publishing):
- `PLAY_SERVICE_ACCOUNT_JSON` - Service Account JSON für Play Store API

---

### 3. Workflow Permissions prüfen

**Schritte:**
1. Gehe zu **Settings** → **Actions** → **General**
2. Scrolle zu "Workflow permissions"
3. Stelle sicher, dass **"Read and write permissions"** aktiviert ist
4. ✅ Aktiviere "Allow GitHub Actions to create and approve pull requests"

Dies ist nötig, damit der Release-Workflow GitHub Releases erstellen kann.

---

### 4. Repository auf Public stellen

**⚠️ Wichtig: Mache das erst NACH dem Branch Protection Setup!**

**Schritte:**
1. Gehe zu **Settings** (Repository-Settings, nicht Account-Settings)
2. Scrolle ganz nach unten zu **"Danger Zone"**
3. Klicke auf **"Change visibility"**
4. Wähle **"Make public"**
5. Bestätige die Aktion

**Vorher prüfen:**
- ✅ Branch Protection ist aktiv
- ✅ Keine Secrets/Credentials im Code committed
- ✅ .gitignore ist korrekt konfiguriert
- ✅ README.md ist aktuell und aussagekräftig
- ✅ LICENSE Datei ist vorhanden

---

## 🚀 Einen Release veröffentlichen

### Schritt-für-Schritt Anleitung

#### 1. Code vorbereiten

Stelle sicher, dass:
- ✅ Alle gewünschten Features/Fixes gemerged sind
- ✅ Der `main` Branch auf dem neuesten Stand ist
- ✅ Alle CI Checks erfolgreich durchlaufen

#### 2. Changelog vorbereiten (optional)

Erstelle eine Liste der Änderungen seit dem letzten Release:
- Neue Features
- Bug Fixes
- Breaking Changes
- Bekannte Issues

**Tipp:** Das Release-Script generiert automatisch ein Changelog aus Git-Commits, aber du kannst es manuell nachbearbeiten.

#### 3. Version Tag erstellen

**Lokale Tags:**

```bash
# Checkout main branch
git checkout main
git pull origin main

# Erstelle einen Tag (z.B. für Version 1.0.0)
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push den Tag zu GitHub
git push origin v1.0.0
```

**Oder über GitHub UI:**

1. Gehe zu **Releases** → **Create a new release**
2. Klicke auf **"Choose a tag"** → **"Create new tag"**
3. Gib den Tag-Namen ein: `v1.0.0` (Format: `v` + Versionsnummer)
4. Wähle den `main` Branch als Target
5. **WICHTIG:** Noch nicht auf "Publish" klicken! Der Workflow erstellt das Release automatisch.

#### 4. Release-Workflow beobachten

1. Gehe zu **Actions** in deinem Repository
2. Der "Release Build" Workflow sollte automatisch gestartet sein
3. Beobachte den Fortschritt (dauert ca. 10-15 Minuten):
   - Quality Checks (ktlint, detekt, lint)
   - Unit Tests
   - APK Build
   - AAB Build
   - Release Creation

#### 5. Release überprüfen

Nach erfolgreichem Workflow-Run:

1. Gehe zu **Releases** in deinem Repository
2. Du solltest einen neuen Release mit Tag `v1.0.0` sehen
3. Überprüfe die Downloads:
   - ✅ `nextcloud-tasks-1.0.0.apk` - Für direkten Download
   - ✅ `nextcloud-tasks-1.0.0.aab` - Für Play Store Upload
4. Überprüfe die Release Notes

#### 6. Release Notes anpassen (optional)

1. Klicke auf **Edit** beim Release
2. Passe die automatisch generierten Release Notes an:
   - Füge Highlights hinzu
   - Gruppiere Änderungen (Features, Bug Fixes, etc.)
   - Füge Screenshots hinzu (falls vorhanden)
   - Füge Breaking Changes oder Migration Notes hinzu
3. Klicke auf **Update release**

---

## 📝 Versioning Schema

Die App verwendet **Semantic Versioning** (SemVer):

```
MAJOR.MINOR.PATCH

Beispiel: 1.2.3
- MAJOR (1): Breaking Changes / Große neue Features
- MINOR (2): Neue Features (rückwärtskompatibel)
- PATCH (3): Bug Fixes (rückwärtskompatibel)
```

**Version Code Berechnung:**
```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

Beispiele:
1.0.0  → 10000
1.2.3  → 10203
2.0.0  → 20000
```

**Git Tag Format:**
- ✅ `v1.0.0` (mit "v" Prefix)
- ❌ `1.0.0` (ohne Prefix)
- ❌ `release-1.0.0`

---

## 🔄 Workflow-Details

### Release Workflow (`.github/workflows/release.yml`)

**Trigger:**
- Bei Push von Tags im Format `v*.*.*` (z.B. `v1.0.0`)
- Manuell über "Run workflow" Button

**Schritte:**
1. **Checkout** - Code auschecken
2. **Setup** - JDK 17, Android SDK, Gradle Cache
3. **Version Extraction** - Version aus Git Tag extrahieren
4. **Quality Checks** - ktlint, detekt, lint (release variant)
5. **Tests** - Unit Tests laufen lassen
6. **Build APK** - Signierte Release-APK bauen
7. **Build AAB** - Signiertes Release-Bundle bauen
8. **Rename** - Dateien mit Version umbenennen
9. **Release Notes** - Automatische Release Notes generieren
10. **Create Release** - GitHub Release mit APK und AAB erstellen

**Outputs:**
- GitHub Release mit Downloads
- Automatische Release Notes aus Git-Commits
- APK und AAB als Release Assets

---

## 🧪 Testen vor dem Release

### Lokales Testen

```bash
# Quality Checks lokal durchführen
./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest

# Signed APK lokal bauen (erfordert Signing Secrets als Env Variables)
export SIGNING_KEYSTORE_BASE64="..."
export SIGNING_KEYSTORE_PASSWORD="..."
export SIGNING_KEY_ALIAS="..."
export SIGNING_KEY_PASSWORD="..."
./gradlew assembleRelease

# APK ist dann hier: app/build/outputs/apk/release/app-release.apk
```

### Test-Release (ohne Tag)

Du kannst den Release-Workflow auch manuell triggern ohne einen Tag zu erstellen:

1. Gehe zu **Actions** → **Release Build**
2. Klicke auf **"Run workflow"**
3. Wähle den Branch (z.B. `main`)
4. Klicke auf **"Run workflow"**

Dies erstellt APK/AAB als **Artifacts** (nicht als Release), die du für 30 Tage herunterladen kannst.

---

## 📱 APK Installation (für User)

Die APK kann direkt auf Android-Geräten installiert werden:

**Voraussetzungen:**
- Android 8.0 (API 26) oder höher
- "Installation aus unbekannten Quellen" aktiviert

**Schritte:**
1. Gehe zum GitHub Release
2. Lade `nextcloud-tasks-X.X.X.apk` herunter
3. Öffne die APK-Datei auf dem Android-Gerät
4. Bestätige die Installation

---

## 🎯 Google Play Store Publishing

Falls du die App später auf Google Play veröffentlichen möchtest:

1. **AAB hochladen:**
   - Lade `nextcloud-tasks-X.X.X.aab` vom Release herunter
   - Gehe zur Google Play Console
   - Upload die AAB-Datei

2. **Automatisches Publishing (optional):**
   - Konfiguriere `PLAY_SERVICE_ACCOUNT_JSON` Secret
   - Der `play-internal` Job in `ci.yml` published dann automatisch zu Internal Track

---

## ❓ Troubleshooting

### Problem: Workflow schlägt bei Signing fehl

**Lösung:** Überprüfe, ob alle Signing Secrets korrekt hinterlegt sind:
```bash
# Secrets müssen in GitHub Settings → Secrets vorhanden sein:
SIGNING_KEYSTORE_BASE64
SIGNING_KEYSTORE_PASSWORD
SIGNING_KEY_ALIAS
SIGNING_KEY_PASSWORD
```

### Problem: Release wird nicht erstellt

**Lösung:** Überprüfe Workflow Permissions:
- Settings → Actions → General → Workflow permissions
- Muss auf "Read and write permissions" stehen

### Problem: Quality Checks schlagen fehl

**Lösung:** Teste lokal vor dem Tag-Push:
```bash
./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest
```

Behebe alle Fehler und Warnings, dann erst Tag pushen.

### Problem: APK kann nicht installiert werden

**Mögliche Ursachen:**
- Android Version zu alt (min. Android 8.0 nötig)
- APK ist nicht korrekt signiert (überprüfe Signing Config)
- Signature conflict mit vorheriger Installation (erst deinstallieren)

---

## 📚 Weitere Informationen

- **CI/CD Workflow:** `.github/workflows/ci.yml` - Läuft bei jedem Push/PR
- **Release Workflow:** `.github/workflows/release.yml` - Läuft bei Version-Tags
- **Build Config:** `app/build.gradle.kts` - Automatische Versionierung
- **Project Docs:** `CLAUDE.md` - Vollständige Projekt-Dokumentation

---

**Bei Fragen oder Problemen:** Erstelle ein Issue auf GitHub!
