# AI Agent Guide - Nextcloud Tasks Android

This document contains essential information for AI agents working on this project to ensure consistency and adherence to project conventions.

## Project Overview

**Nextcloud Tasks Android** is a modular Android application for managing Nextcloud Tasks, built with modern Android development practices.

- **Package Name**: `com.nextcloud.tasks`
- **Min SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 34 (Android 14)
- **Language**: Kotlin
- **Java Version**: 17

## Architecture

### Module Structure (Clean Architecture)

The project follows Clean Architecture principles with three distinct modules:

1. **`:domain`** (Pure Kotlin Module)
   - Location: `/domain`
   - Contains: Business logic, interfaces, use cases, domain models
   - Dependencies: Only `kotlinx-coroutines-core` (no Android dependencies)
   - Package: `com.nextcloud.tasks.domain`
   - Example: `Task` data class, `TasksRepository` interface, `LoadTasksUseCase`

2. **`:data`** (Android Library)
   - Location: `/data`
   - Contains: Repository implementations, data sources, data models
   - Dependencies: `:domain`, Hilt, Coroutines
   - Package: `com.nextcloud.tasks.data`
   - Example: `DefaultTasksRepository` implementation

3. **`:app`** (Android Application)
   - Location: `/app`
   - Contains: UI (Jetpack Compose), ViewModels, Dependency Injection setup, themes
   - Dependencies: `:domain`, `:data`, Compose, Hilt, Material 3
   - Package: `com.nextcloud.tasks`
   - Entry Point: `TasksApp` (Application class with `@HiltAndroidApp`)

### Dependency Flow

```
:app → :data → :domain
```

- `:domain` has no dependencies on other modules
- `:data` depends only on `:domain`
- `:app` depends on both `:data` and `:domain`

## Dependency Injection

**Framework**: Dagger Hilt

### Key Components

- **Application Class**: `TasksApp` annotated with `@HiltAndroidApp`
- **Activities**: Annotated with `@AndroidEntryPoint` (e.g., `MainActivity`)
- **ViewModels**: Annotated with `@HiltViewModel`

### Hilt Modules

1. **AppModule** (`app/src/main/java/com/nextcloud/tasks/di/AppModule.kt`)
   - Installed in `SingletonComponent`
   - Provides use cases
   - Uses `@Provides` with `@Singleton`

2. **DataModule** (`data/src/main/kotlin/com/nextcloud/tasks/data/di/DataModule.kt`)
   - Installed in `SingletonComponent`
   - Binds repository implementations
   - Uses `@Binds` with `@Singleton`

### DI Conventions

- Use `@Inject constructor()` for repository/use case injection
- Use `object` for `@Provides` modules
- Use `interface` for `@Binds` modules
- Always mark singleton dependencies with `@Singleton`

## UI & Theming

### UI Framework

- **Jetpack Compose** with Material 3
- **Kotlin Compiler Extension Version**: 1.5.15
- **Compose BOM**: 2024.10.00

### Theme Structure

- **Theme File**: `app/src/main/java/com/nextcloud/tasks/ui/theme/Theme.kt`
- **Theme Function**: `NextcloudTasksTheme`
- **Features**:
  - Light/Dark theme support (follows system preference)
  - Dynamic color support for Android 12+ (Material You)
  - Custom Nextcloud colors for older devices
  - Automatic status bar and navigation bar styling

### UI Conventions

- Use `MaterialTheme.colorScheme` for colors
- Use `MaterialTheme.typography` for text styles
- Use `MaterialTheme.shapes` for shapes
- Prefer `Surface` with `tonalElevation` over `shadowElevation`
- Use `Scaffold` for screen-level layouts with `TopAppBar`
- Organize Composables: Screen → List → Item pattern
- Keep ViewModels close to Activities (same file for simple cases)

## Code Style & Quality

### Linting & Static Analysis

1. **ktlint** (Kotlin code style)
   - Configuration: Applied in root `build.gradle.kts`
   - Android mode enabled
   - Excludes generated code
   - Command: `./gradlew ktlintCheck`

2. **detekt** (Static analysis)
   - Configuration: `config/detekt/detekt.yml`
   - Auto-correct enabled
   - MagicNumber check disabled
   - Command: `./gradlew detekt`

3. **Android Lint**
   - Command: `./gradlew :app:lintDebug`

### Code Conventions

- **Code Style**: Official Kotlin code style (`kotlin.code.style=official`)
- **File Organization**: Package by feature, then by layer
- **Naming**:
  - Classes: PascalCase
  - Functions/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Files: Match the primary class name
- **Imports**: No wildcard imports
- **ViewModels**: Suffix with `ViewModel` (e.g., `TaskListViewModel`)
- **Use Cases**: Suffix with `UseCase` (e.g., `LoadTasksUseCase`)
- **Repositories**: Suffix with `Repository`, prefix implementation with `Default` (e.g., `DefaultTasksRepository`)

### Kotlin Conventions

- Use `data class` for models
- Use `Flow` for reactive data streams
- Prefer `StateFlow` for state management in repositories
- Use `stateIn()` to convert Flow to StateFlow in ViewModels
- Use `suspend` functions for one-shot operations
- Prefer `@Composable` functions over Views

## Build & Test

### Gradle Commands

```bash
# Code Quality
./gradlew ktlintCheck          # Check code style
./gradlew detekt               # Run static analysis
./gradlew :app:lintDebug       # Run Android lint

# Testing
./gradlew testDebugUnitTest    # Run unit tests

# Building
./gradlew assembleDebug        # Build debug APK
./gradlew bundleRelease        # Build release AAB

# Play Store
./gradlew publishReleaseBundle # Upload to Play Store (internal track)
```

### Build Configuration

- **Kotlin JVM Target**: 17
- **Java Compatibility**: VERSION_17
- **Kapt**: `correctErrorTypes = true`
- **ProGuard**: Disabled for now (commented out in build files)
- **Build Types**:
  - Debug: `applicationIdSuffix = ".debug"`, no minification
  - Release: Signed, minification disabled for now

### Signing (Release)

Release builds are signed using environment variables:
- `SIGNING_KEYSTORE_BASE64`: Base64-encoded keystore
- `SIGNING_KEYSTORE_PASSWORD`: Keystore password
- `SIGNING_KEY_ALIAS`: Key alias
- `SIGNING_KEY_PASSWORD`: Key password

Keystore is decoded and written to `build/keystore/release.jks` at build time.

## CI/CD

### GitHub Actions

**Workflow File**: `.github/workflows/ci.yml`

**Jobs**:

1. **quality** (runs on all pushes/PRs)
   - ktlint check
   - detekt analysis
   - Android lint (debug)
   - Unit tests

2. **signed-build** (runs after quality, only on main branch)
   - Builds signed release bundle
   - Uploads artifact

3. **play-internal** (optional, if secrets configured)
   - Publishes to Play Store internal track

**Environment**:
- Java: Temurin 17
- Android SDK: via `android-actions/setup-android@v3`
- Gradle caching: enabled

### Fastlane

**Location**: `fastlane/Fastfile`

**Lane**: `internal`
- Builds release bundle
- Publishes to Play Store via Gradle Play Publisher

## Dependencies

### Version Catalog

**File**: `gradle/libs.versions.toml`

**Key Libraries**:
- Compose BOM: 2024.10.00
- Kotlin: 1.9.25
- Hilt: 2.52
- Coroutines: 1.9.0
- Android Gradle Plugin: 8.13.2

### Dependency Management

- Use version catalog (`libs.versions.toml`) for all dependencies
- Access via `libs.` prefix in build files
- Group related dependencies in bundles (e.g., `bundles.compose`)

## Play Store Integration

**Plugin**: Gradle Play Publisher (3.9.1)

**Configuration** (in `app/build.gradle.kts`):
- Track: `internal`
- Format: AAB (App Bundles)
- Resolution Strategy: `AUTO`
- Service Account: via `PLAY_SERVICE_ACCOUNT_JSON` environment variable

## Important Patterns

### Repository Pattern

```kotlin
// Domain (interface)
interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addSampleTasksIfEmpty()
}

// Data (implementation)
class DefaultTasksRepository @Inject constructor() : TasksRepository {
    override fun observeTasks(): Flow<List<Task>> = // ...
}
```

### Use Case Pattern

```kotlin
class LoadTasksUseCase @Inject constructor(
    private val repository: TasksRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
    suspend fun seedSample() = repository.addSampleTasksIfEmpty()
}
```

### ViewModel Pattern

```kotlin
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val loadTasksUseCase: LoadTasksUseCase
) : ViewModel() {
    val tasks = loadTasksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
```

### Compose Screen Pattern

```kotlin
@Composable
fun TaskListScreen(viewModel: TaskListViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    
    Scaffold(topBar = { /* ... */ }) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(padding)
        } else {
            TaskList(padding, tasks)
        }
    }
}
```

## File Structure Conventions

### Package Structure

```
com.nextcloud.tasks/
├── (root)              # Application, MainActivity, ViewModels
├── di/                 # Hilt modules (AppModule)
└── ui/
    └── theme/          # Theme.kt, Color.kt, Type.kt

com.nextcloud.tasks.data/
├── di/                 # Hilt modules (DataModule)
└── repository/         # Repository implementations

com.nextcloud.tasks.domain/
├── model/              # Domain models (Task)
├── repository/         # Repository interfaces
└── usecase/            # Use cases
```

### Source Sets

- **App**: `app/src/main/java/` (note: Java directory even for Kotlin files)
- **Data**: `data/src/main/kotlin/`
- **Domain**: `domain/src/main/kotlin/`

## Important Notes for AI Agents

### DO's

✅ Follow Clean Architecture separation (domain → data → app)
✅ Use Hilt for dependency injection
✅ Use Flow for reactive streams
✅ Use Jetpack Compose for UI
✅ Follow Material 3 guidelines
✅ Run ktlint, detekt, and lint before committing
✅ Write unit tests (testDebugUnitTest)
✅ Use the version catalog for dependencies
✅ Keep ViewModels framework-agnostic (except Android lifecycle)
✅ Use suspend functions for async operations
✅ Prefer immutable data classes

### DON'Ts

❌ Don't add Android dependencies to `:domain` module
❌ Don't use Views/XML layouts (use Compose)
❌ Don't bypass Hilt for dependency management
❌ Don't use wildcard imports
❌ Don't commit keystore files or secrets
❌ Don't skip linting/static analysis
❌ Don't use deprecated Compose APIs
❌ Don't add dependencies without updating version catalog
❌ Don't couple domain logic to UI or data layers
❌ Don't use blocking calls on main thread

## Testing

### Current Test Structure

- Unit tests: `testDebugUnitTest`
- Framework: Kotlin Test (`kotlin("test")`)
- Location: `src/test/` directories in each module

### Testing Conventions

- Test repository implementations with fake data
- Test use cases with mock repositories
- Test ViewModels with test coroutines
- Use `kotlinx-coroutines-test` for testing suspending functions

## Accessibility

The app is designed with accessibility in mind:
- Semantic content descriptions where needed
- Proper contrast ratios (Material 3 handles this)
- Dynamic color support
- Scalable text (using MaterialTheme.typography)

## Future Considerations

- Database integration (Room) will go in `:data` module
- Network layer (Retrofit/Ktor) will go in `:data` module
- Feature modules can be added following the same pattern
- UI testing with Compose testing library

---

**Last Updated**: 2025-12-14
**Project Version**: 1.0.0

For questions or updates to this guide, please ensure consistency with the actual codebase and update the "Last Updated" timestamp.
