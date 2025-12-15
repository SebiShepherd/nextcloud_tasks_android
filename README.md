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

### Wichtige Befehle
- Format/Analyse: `./gradlew ktlintCheck detekt`
- Lint: `./gradlew :app:lintDebug`
- Tests: `./gradlew testDebugUnitTest`
- Release-Bundle: `./gradlew bundleRelease`
- Play-Upload (intern, setzt `PLAY_SERVICE_ACCOUNT_JSON` voraus): `./gradlew publishReleaseBundle`

## Debugging von Login-/Auth-Problemen

- Verwende die **debug**-Variante der App. Timber-Logs und der OkHttp-Logging-Interceptor werden nur in Debug-Builds aktiviert.
- Öffne in Android Studio die **Logcat**-Ansicht, während die Debug-App läuft. Filtere nach Package `com.nextcloud.tasks` oder Tag `LoginViewModel`, um Validierungs-, Submit- und Fehler-Logs zu sehen. HTTP-Anfrage-/Antwort-Header werden dort ebenfalls ausgegeben (sensibles bleibt ausgelassen).
- Falls in Logcat eine Meldung wie `SecurityException: Permission denied (missing INTERNET permission?)` erscheint, stelle sicher, dass die App nach Manifest-Änderungen neu installiert wurde und die **INTERNET**-Berechtigung enthält.
- Fehlertexte aus dem UI stammen aus `LoginUiState.error`. Wenn ein Fehler angezeigt wird, bitte einen Screenshot plus passenden Logcat-Ausschnitt anhängen.

## CI
GitHub Actions Workflow (`.github/workflows/ci.yml`):
1. `quality`: ktlint, detekt, Lint, Unit-Tests.
2. `signed-build`: signiertes `bundleRelease` mit Keystore-Secrets.
3. `play-internal`: optionaler Upload in den internen Play-Track, falls Service-Account-Secret vorhanden ist.

## Fastlane
`fastlane/Fastfile` enthält eine Lane `internal`, die das Release-Bundle baut und (mit korrekten Umgebungsvariablen) über Gradle Play Publisher hochladen kann.
