# Nextcloud Tasks Android

Ein modularer Android-Client für Nextcloud Tasks mit Jetpack Compose, Hilt und vorbereiteter Play-Publisher-Integration.

## Projektaufbau
- **app**: Android-App mit Compose UI, Hilt-Setup, Play-Publisher-Konfiguration und Basis-Theming (Light/Dark, Dynamic Color ab Android 12).
- **data**: Android-Library mit Hilt-Modul und Repository-Implementierung.
- **domain**: Reines Kotlin-Modul mit Business-Interfaces und Use-Cases.

## Entwicklung
- Min. SDK: 24, Target/Compile SDK: 34.
- App-ID/Paket: `com.nextcloud.tasks`.
- Dependency Injection: Hilt (`TasksApp`, Hilt-Module in `app/di` und `data/di`).
- UI: Jetpack Compose (Material 3) mit zugänglichem Light/Dark-Theme.

### OAuth-Konfiguration
Die App verwendet OAuth für die Nextcloud-Authentifizierung. Die OAuth-Client-Credentials können über folgende Methoden konfiguriert werden:

1. **Umgebungsvariablen** (empfohlen für CI/CD):
   ```bash
   export OAUTH_CLIENT_ID="your-client-id"
   export OAUTH_CLIENT_SECRET="your-client-secret"
   ```

2. **gradle.properties** (lokal):
   ```properties
   OAUTH_CLIENT_ID=your-client-id
   OAUTH_CLIENT_SECRET=your-client-secret
   ```

3. **Standardwerte**: Ohne Konfiguration werden Platzhalter-Werte für lokale Entwicklung verwendet (`nextcloud-tasks-android` / `local-client-secret`).

**Wichtig**: Niemals echte OAuth-Credentials in den Quellcode committen!

### Wichtige Befehle
- Format/Analyse: `./gradlew ktlintCheck detekt`
- Lint: `./gradlew :app:lintDebug`
- Tests: `./gradlew testDebugUnitTest`
- Release-Bundle: `./gradlew bundleRelease`
- Play-Upload (intern, setzt `PLAY_SERVICE_ACCOUNT_JSON` voraus): `./gradlew publishReleaseBundle`

## Debugging von Login-/Auth-Problemen

- Verwende die **debug**-Variante der App. Timber-Logs und der OkHttp-Logging-Interceptor werden nur in Debug-Builds aktiviert.
- Öffne in Android Studio die **Logcat**-Ansicht, während die Debug-App läuft. Filtere nach Package `com.nextcloud.tasks` oder Tag `LoginViewModel`, um Validierungs-, Submit- und Fehler-Logs zu sehen. HTTP-Anfrage-/Antwort-Header werden dort ebenfalls ausgegeben (sensibles bleibt ausgelassen).
- Die App deklariert die **INTERNET**-Berechtigung im `app`- und `data`-Modul. Falls Logcat dennoch `SecurityException: Permission denied (missing INTERNET permission?)` zeigt, deinstalliere die vorhandene App vollständig und installiere die Debug-Variante neu, damit die Berechtigung übernommen wird. Firewalls/VPNs können ebenfalls DNS-Lookups blockieren und denselben Fehler auslösen.
- Fehlertexte aus dem UI stammen aus `LoginUiState.error`. Wenn ein Fehler angezeigt wird, bitte einen Screenshot plus passenden Logcat-Ausschnitt anhängen.

## CI
GitHub Actions Workflow (`.github/workflows/ci.yml`):
1. `quality`: ktlint, detekt, Lint, Unit-Tests.
2. `signed-build`: signiertes `bundleRelease` mit Keystore-Secrets.
3. `play-internal`: optionaler Upload in den internen Play-Track, falls Service-Account-Secret vorhanden ist.

## Fastlane
`fastlane/Fastfile` enthält eine Lane `internal`, die das Release-Bundle baut und (mit korrekten Umgebungsvariablen) über Gradle Play Publisher hochladen kann.
