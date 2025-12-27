# CLAUDE.md - AI Assistant Guide for Nextcloud Tasks Android

**Last Updated**: 2025-12-27
**Project Version**: 1.0.0
**Target Audience**: Claude Code and AI assistants working on this codebase

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Key Technologies](#key-technologies)
4. [Code Structure](#code-structure)
5. [Development Workflows](#development-workflows)
6. [Code Conventions](#code-conventions)
7. [Important Patterns](#important-patterns)
8. [Build & Deployment](#build--deployment)
9. [Testing](#testing)
10. [AI Assistant Guidelines](#ai-assistant-guidelines)
11. [Common Tasks](#common-tasks)

---

## Project Overview

**Nextcloud Tasks Android** is a native Android client for Nextcloud Tasks, built with modern Android development practices and Clean Architecture principles.

### Key Information

- **Package Name**: `com.nextcloud.tasks`
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 36 (Android 15)
- **Java Version**: 17
- **Module Count**: 3 (app, data, domain)
- **Total Kotlin Files**: ~67

### Features

- Multi-account support with account switching
- CalDAV-based task synchronization
- VTODO (iCalendar) parsing and generation
- Offline-first architecture with Room database
- Pull-to-refresh synchronization
- Task lists with color coding
- Task filtering and sorting
- Material 3 design with dynamic theming

---

## Architecture

### Clean Architecture (3-Layer)

The project follows Clean Architecture with strict dependency rules:

```
┌─────────────────────────────────────────────────┐
│ :app (Android Application)                     │
│ • UI (Jetpack Compose)                         │
│ • ViewModels                                   │
│ • Navigation                                   │
│ • Dependency Injection Setup                   │
└─────────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────────┐
│ :data (Android Library)                        │
│ • Repository Implementations                   │
│ • Room Database                                │
│ • CalDAV Service                               │
│ • Network Layer (Retrofit + OkHttp)            │
│ • iCal4j Integration (VTODO parsing/generation)│
│ • Authentication & Token Management            │
└─────────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────────┐
│ :domain (Pure Kotlin Module)                   │
│ • Business Models (Task, TaskList, Tag, etc.)  │
│ • Repository Interfaces                        │
│ • Use Cases                                    │
│ • NO Android dependencies                      │
└─────────────────────────────────────────────────┘
```

### Module Details

#### `:app` Module
- **Path**: `/app`
- **Package**: `com.nextcloud.tasks`
- **Source**: `app/src/main/java/` (uses 'java' directory for Kotlin)
- **Purpose**: UI layer with Jetpack Compose screens
- **Key Files**:
  - `MainActivity.kt` - Entry point with all UI composables
  - `TasksApp.kt` - Application class with `@HiltAndroidApp`
  - `auth/LoginScreen.kt`, `auth/LoginViewModel.kt` - Authentication UI
  - `di/AppModule.kt` - Hilt module for use cases
  - `ui/theme/Theme.kt`, `Color.kt`, `Type.kt` - Material 3 theming

#### `:data` Module
- **Path**: `/data`
- **Package**: `com.nextcloud.tasks.data`
- **Source**: `data/src/main/kotlin/`
- **Purpose**: Data layer with repositories, database, and network
- **Key Subdirectories**:
  - `api/` - Retrofit API interfaces and DTOs
  - `caldav/` - CalDAV service, parsers (VTodoParser, DavMultistatusParser), generators (VTodoGenerator)
  - `database/` - Room database, DAOs, entities
  - `repository/` - Repository implementations (DefaultTasksRepository, DefaultAuthRepository)
  - `network/` - OkHttp clients, interceptors, SafeDns
  - `auth/` - Authentication token provider
  - `mapper/` - Entity ↔ Domain model mappers
  - `di/` - Hilt modules (DataModule, NetworkModule)

#### `:domain` Module
- **Path**: `/domain`
- **Package**: `com.nextcloud.tasks.domain`
- **Source**: `domain/src/main/kotlin/`
- **Purpose**: Pure business logic (platform-agnostic)
- **Key Subdirectories**:
  - `model/` - Domain models (Task, TaskList, Tag, NextcloudAccount, etc.)
  - `repository/` - Repository interfaces (TasksRepository, AuthRepository)
  - `usecase/` - Use cases (LoadTasksUseCase, LoginWithPasswordUseCase, etc.)

---

## Key Technologies

### Core Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 2.1.10 | Primary language |
| **Jetpack Compose** | BOM 2025.12.00 | UI framework |
| **Material 3** | 1.4.0 | Design system |
| **Hilt** | 2.57.2 | Dependency injection |
| **Room** | 2.7.0-alpha04 | Local database |
| **Retrofit** | 2.11.0 | REST API client |
| **OkHttp** | 4.12.0 | HTTP client |
| **Coroutines** | 1.10.2 | Async programming |
| **iCal4j** | 3.2.18 | iCalendar VTODO parsing/generation |
| **Timber** | 5.0.1 | Logging |
| **Coil** | 2.7.0 | Image loading (avatars) |

### Build Tools

- **Gradle**: 8.13.2 (Android Gradle Plugin)
- **KSP**: 2.1.10-1.0.29 (for Room)
- **Kapt**: Used for Hilt
- **ktlint**: 14.0.1 (code formatting)
- **detekt**: 1.23.8 (static analysis)

### CalDAV Integration

The app uses **CalDAV protocol** for task synchronization:
- **iCal4j** library for parsing/generating VTODO (iCalendar tasks)
- Custom `CalDavService` for HTTP PROPFIND/REPORT requests
- `VTodoParser` for parsing VCALENDAR → Task entities
- `VTodoGenerator` for generating Task → VCALENDAR strings
- ETags for optimistic locking during updates

---

## Code Structure

### Package Organization

```
com.nextcloud.tasks/
├── MainActivity.kt              # Main activity with all Compose screens
├── TasksApp.kt                  # Application class (@HiltAndroidApp)
├── auth/
│   ├── LoginScreen.kt           # Login UI
│   ├── LoginViewModel.kt        # Login business logic
│   └── LoginCallbacks.kt        # Login event callbacks
├── di/
│   └── AppModule.kt             # Hilt module (provides use cases)
└── ui/
    └── theme/
        ├── Theme.kt             # NextcloudTasksTheme
        ├── Color.kt             # Color definitions
        └── Type.kt              # Typography

com.nextcloud.tasks.data/
├── api/
│   ├── NextcloudTasksApi.kt     # Retrofit API interface
│   └── dto/                     # Data Transfer Objects
├── caldav/
│   ├── service/
│   │   └── CalDavService.kt     # CalDAV HTTP operations
│   ├── parser/
│   │   ├── VTodoParser.kt       # VTODO → TaskEntity
│   │   └── DavMultistatusParser.kt
│   ├── generator/
│   │   └── VTodoGenerator.kt    # Task → VTODO
│   └── models/
│       ├── DavResponse.kt
│       └── DavProperty.kt
├── database/
│   ├── NextcloudTasksDatabase.kt
│   ├── dao/                     # Room DAOs
│   ├── entity/                  # Room entities
│   ├── model/                   # Relation models (TaskWithRelations)
│   └── converter/               # Type converters (InstantTypeConverter)
├── repository/
│   ├── DefaultTasksRepository.kt
│   └── DefaultAuthRepository.kt
├── network/
│   ├── NextcloudService.kt
│   ├── NextcloudClientFactory.kt
│   ├── AuthenticationInterceptors.kt
│   └── SafeDns.kt
├── auth/
│   ├── AuthToken.kt
│   └── AuthTokenProvider.kt     # Token management
├── mapper/
│   ├── TaskMapper.kt            # Entity ↔ Domain
│   ├── TaskListMapper.kt
│   └── TagMapper.kt
├── sync/
│   └── SyncManager.kt
└── di/
    ├── DataModule.kt            # Provides DB, Retrofit, CalDAV
    └── NetworkModule.kt         # Provides OkHttp, interceptors

com.nextcloud.tasks.domain/
├── model/
│   ├── Task.kt                  # Core task model
│   ├── TaskDraft.kt             # For creating tasks
│   ├── TaskList.kt
│   ├── Tag.kt
│   ├── NextcloudAccount.kt
│   ├── TaskFilter.kt            # ALL, CURRENT, COMPLETED
│   ├── TaskSort.kt              # DUE_DATE, PRIORITY, TITLE, UPDATED_AT
│   ├── AuthType.kt
│   └── AuthFailure.kt
├── repository/
│   ├── TasksRepository.kt       # Interface for task operations
│   └── AuthRepository.kt        # Interface for auth operations
└── usecase/
    ├── LoadTasksUseCase.kt
    ├── LoginWithPasswordUseCase.kt
    ├── LoginWithOAuthUseCase.kt
    ├── ObserveActiveAccountUseCase.kt
    ├── ObserveAccountsUseCase.kt
    ├── SwitchAccountUseCase.kt
    ├── LogoutUseCase.kt
    └── ValidateServerUrlUseCase.kt
```

---

## Development Workflows

### Code Quality Checks (Always Run Before Committing)

```bash
# Format check
./gradlew ktlintCheck

# Static analysis
./gradlew detekt

# Android lint
./gradlew :app:lintDebug

# Unit tests
./gradlew testDebugUnitTest

# All quality checks together
./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest
```

### Building

```bash
# Debug APK
./gradlew assembleDebug

# Debug with installation
./gradlew installDebug

# Release bundle (requires signing secrets)
./gradlew bundleRelease

# Clean build
./gradlew clean assembleDebug
```

### Play Store Publishing

```bash
# Upload to internal track (requires PLAY_SERVICE_ACCOUNT_JSON)
./gradlew publishReleaseBundle

# Or via Fastlane
fastlane internal
```

### Debugging

- **Debug variant** enables Timber logging and OkHttp logging interceptor
- **Application ID**: `com.nextcloud.tasks.debug` (debug), `com.nextcloud.tasks` (release)
- **Logcat filters**:
  - Package: `com.nextcloud.tasks`
  - Tag: `LoginViewModel`, `CalDavService`, `VTodoParser`, etc.

---

## Code Conventions

### Kotlin Style

- **Code Style**: Official Kotlin style (`kotlin.code.style=official`)
- **Max Line Length**: Enforced by ktlint
- **No wildcard imports**: Forbidden
- **EditorConfig**: Special rule for Composable function naming

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| **Classes** | PascalCase | `TaskListViewModel` |
| **Functions/Variables** | camelCase | `observeTasks()`, `isRefreshing` |
| **Constants** | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT` |
| **Composables** | PascalCase | `TaskCard()`, `EmptyState()` |
| **Files** | Match primary class | `TasksRepository.kt` |
| **Repositories** | `Default` prefix for impl | `DefaultTasksRepository` |
| **Use Cases** | Suffix with `UseCase` | `LoadTasksUseCase` |
| **ViewModels** | Suffix with `ViewModel` | `LoginViewModel` |
| **DTOs** | Suffix with `Dto` | `TaskDto`, `TaskRequestDto` |
| **Entities** | Suffix with `Entity` | `TaskEntity`, `TaskListEntity` |

### File Organization

- **Package by feature, then by layer**
- **Composables can be co-located** with ViewModels if tightly coupled (see `MainActivity.kt`)
- **Mappers** in separate package (`data/mapper/`)
- **One public class per file** (except sealed classes and tightly coupled Composables)

### Detekt Rules

Key configurations from `config/detekt/detekt.yml`:

- ❌ **MagicNumber**: Disabled (numbers in Compose are common)
- ✅ **ReturnCount**: Max 3
- ✅ **LongParameterList**: Ignores `@Composable`
- ✅ **LongMethod**: Max 70 lines, ignores `@Composable`
- ✅ **TooManyFunctions**: Max 15 per file/class
- ✅ **CyclomaticComplexMethod**: Max 20
- ✅ **FunctionNaming**: Ignores `@Composable` (allows PascalCase)
- ✅ **Formatting**: Auto-correct enabled
- ❌ **Indentation**: Disabled (ktlint handles it)

### Suppression Annotations

Use `@Suppress` sparingly and only when necessary:

```kotlin
@Suppress("TooManyFunctions")
interface TasksRepository {
    // ... many methods
}

@Suppress("LongMethod")
override suspend fun refresh() {
    // Complex sync logic
}

@Suppress("UnusedParameter")
@Composable
private fun TasksContent(padding: PaddingValues, ...) {
    // Padding required by Scaffold but not used yet
}
```

---

## Important Patterns

### 1. Repository Pattern

**Domain (Interface)**:
```kotlin
// domain/repository/TasksRepository.kt
interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun createTask(draft: TaskDraft): Task
    suspend fun updateTask(task: Task): Task
    suspend fun deleteTask(taskId: String)
    suspend fun refresh()
}
```

**Data (Implementation)**:
```kotlin
// data/repository/DefaultTasksRepository.kt
class DefaultTasksRepository @Inject constructor(
    private val database: NextcloudTasksDatabase,
    private val calDavService: CalDavService,
    private val vTodoParser: VTodoParser,
    private val vTodoGenerator: VTodoGenerator,
    private val authTokenProvider: AuthTokenProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TasksRepository {

    override fun observeTasks(): Flow<List<Task>> =
        authTokenProvider
            .observeActiveAccountId()
            .flatMapLatest { accountId ->
                if (accountId != null) {
                    database.tasksDao().observeTasks(accountId)
                        .map { tasks -> tasks.map(taskMapper::toDomain) }
                } else {
                    flowOf(emptyList())
                }
            }

    override suspend fun createTask(draft: TaskDraft): Task =
        withContext(ioDispatcher) {
            // 1. Generate VTODO string
            val icalData = vTodoGenerator.generateVTodo(task)

            // 2. Upload to CalDAV server
            calDavService.createTodo(baseUrl, listId, filename, icalData)

            // 3. Save to local database
            database.tasksDao().upsertTask(taskEntity)

            // 4. Return created task
            getTask(taskEntity.id) ?: error("Task not found")
        }
}
```

### 2. Use Case Pattern

```kotlin
// domain/usecase/LoadTasksUseCase.kt
class LoadTasksUseCase @Inject constructor(
    private val repository: TasksRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
}
```

### 3. ViewModel Pattern

```kotlin
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val loadTasksUseCase: LoadTasksUseCase,
    private val tasksRepository: TasksRepository,
) : ViewModel() {

    val tasks = loadTasksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                tasksRepository.refresh()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
```

### 4. Compose Screen Pattern

```kotlin
@Composable
fun TaskListScreen(viewModel: TaskListViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        floatingActionButton = { /* FAB */ }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh
        ) {
            TaskList(tasks = tasks)
        }
    }
}
```

### 5. Hilt Module Patterns

**AppModule (Provides)**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideLoadTasksUseCase(repository: TasksRepository): LoadTasksUseCase =
        LoadTasksUseCase(repository)
}
```

**DataModule (Binds)**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryBindings {
    @Binds
    @Singleton
    fun bindTasksRepository(impl: DefaultTasksRepository): TasksRepository
}
```

### 6. CalDAV Sync Pattern

```kotlin
override suspend fun refresh() = withContext(ioDispatcher) {
    // 1. CalDAV Discovery (PROPFIND)
    val principal = calDavService.discoverPrincipal(baseUrl).getOrThrow()
    val calendarHome = calDavService.discoverCalendarHome(baseUrl, principal.principalUrl).getOrThrow()

    // 2. Enumerate Collections (PROPFIND)
    val collections = calDavService.enumerateCalendarCollections(baseUrl, calendarHome.calendarHomeUrl).getOrThrow()

    // 3. Fetch VTODOs (REPORT)
    collections.forEach { collection ->
        val todos = calDavService.fetchTodosFromCollection(baseUrl, collection.href).getOrThrow()
        todos.forEach { todo ->
            val taskEntity = vTodoParser.parseVTodo(todo.calendarData, accountId, collection.href, todo.href, todo.etag)
            allTasks.add(taskEntity)
        }
    }

    // 4. Update database
    database.withTransaction {
        upsertTaskLists(taskLists)
        upsertTasksFromCalDav(allTasks)
    }
}
```

### 7. Multi-Account Support Pattern

```kotlin
// Auth token provider tracks active account
interface AuthTokenProvider {
    fun observeActiveAccountId(): Flow<String?>
    suspend fun activeAccountId(): String?
    suspend fun activeServerUrl(): String?
}

// Repository filters by active account
override fun observeTasks(): Flow<List<Task>> =
    authTokenProvider
        .observeActiveAccountId()
        .flatMapLatest { accountId ->
            if (accountId != null) {
                tasksDao.observeTasks(accountId).map { ... }
            } else {
                flowOf(emptyList())
            }
        }
```

---

## Build & Deployment

### Build Variants

| Variant | App ID | Minification | Signing | Use Case |
|---------|--------|--------------|---------|----------|
| **Debug** | `com.nextcloud.tasks.debug` | No | Debug keystore | Development, testing |
| **Release** | `com.nextcloud.tasks` | No (disabled) | Release keystore | Production |

### Signing Configuration

Release builds require environment variables:

```bash
export SIGNING_KEYSTORE_BASE64="..."  # Base64-encoded keystore
export SIGNING_KEYSTORE_PASSWORD="..."
export SIGNING_KEY_ALIAS="..."
export SIGNING_KEY_PASSWORD="..."
```

The keystore is decoded at build time to `app/build/keystore/release.jks`.

### CI/CD (GitHub Actions)

**Workflow**: `.github/workflows/ci.yml`

**Jobs**:

1. **quality** (runs on all pushes/PRs)
   - ktlint check
   - detekt analysis
   - Android lint (debug)
   - Unit tests

2. **signed-build** (runs after quality, only on main branch)
   - Builds signed release bundle
   - Uploads AAB artifact

3. **play-internal** (optional, if secrets configured)
   - Publishes to Play Store internal track
   - Requires `PLAY_SERVICE_ACCOUNT_JSON` secret

**Environment**:
- Java: Temurin 17
- Gradle caching: Enabled
- Android SDK: Setup via `android-actions/setup-android@v3`

### Fastlane

**Lane**: `internal`

```ruby
platform :android do
  desc "Build and upload an internal release via Gradle Play Publisher"
  lane :internal do
    gradle(task: "bundle", build_type: "Release")
    gradle(task: "publishReleaseBundle")
  end
end
```

---

## Testing

### Test Structure

```
src/test/kotlin/          # Unit tests
src/androidTest/kotlin/   # Instrumentation tests (future)
```

### Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific module
./gradlew :domain:test
./gradlew :data:testDebugUnitTest
./gradlew :app:testDebugUnitTest
```

### Testing Conventions

- Use `kotlin("test")` framework
- Test repository implementations with fake data
- Test use cases with mock repositories
- Test ViewModels with `kotlinx-coroutines-test`
- Test Composables with `androidx.compose.ui.test`

### Test File Naming

- Test file: `TasksRepositoryTest.kt`
- Test class: `class TasksRepositoryTest { ... }`
- Test function: `fun `should create task when draft is valid`() { ... }`

---

## AI Assistant Guidelines

### ✅ DO's

1. **Follow Clean Architecture**
   - Never add Android dependencies to `:domain`
   - Keep data sources in `:data`, business logic in `:domain`, UI in `:app`
   - Use interfaces in domain, implementations in data

2. **Use Dependency Injection**
   - Always use Hilt for DI
   - Use `@Inject constructor()` for repositories and use cases
   - Add `@HiltViewModel` to ViewModels
   - Add `@AndroidEntryPoint` to Activities

3. **Code Quality**
   - Run `ktlintCheck`, `detekt`, and `lintDebug` before committing
   - Fix all warnings and errors
   - Use `@Suppress` only when absolutely necessary with clear justification

4. **Async Operations**
   - Use `suspend` functions for one-shot operations
   - Use `Flow` for streams of data
   - Use `viewModelScope` in ViewModels
   - Use `withContext(ioDispatcher)` for IO operations in repositories

5. **Compose Best Practices**
   - Use Material 3 components
   - Use `MaterialTheme.colorScheme`, `typography`, `shapes`
   - Prefer `StateFlow.collectAsState()` in Composables
   - Use `remember` for state, `derivedStateOf` for computed state

6. **CalDAV Integration**
   - Use `VTodoParser` for parsing VCALENDAR → TaskEntity
   - Use `VTodoGenerator` for generating Task → VCALENDAR
   - Handle ETags for optimistic locking
   - Always sync to server before updating local database

7. **Database Operations**
   - Use `database.withTransaction { }` for multiple operations
   - Use `upsert` instead of insert/update when appropriate
   - Use Room's `@Transaction` for complex queries
   - Use `InstantTypeConverter` for `Instant` fields

8. **Error Handling**
   - Log errors with Timber (e.g., `Timber.e(exception, "Error message")`)
   - Use `Result<T>` for CalDAV service operations
   - Catch and handle exceptions in ViewModels
   - Show user-friendly error messages in UI

9. **Multi-Account Support**
   - Always filter data by `accountId`
   - Use `AuthTokenProvider` to get active account
   - Clear account data on logout

### ❌ DON'Ts

1. **Architecture Violations**
   - ❌ Add Android dependencies to `:domain`
   - ❌ Use data layer classes directly in UI
   - ❌ Put business logic in ViewModels (use Use Cases)
   - ❌ Bypass Hilt for dependency management

2. **Code Style**
   - ❌ Use wildcard imports
   - ❌ Commit without running quality checks
   - ❌ Skip linting/static analysis
   - ❌ Hardcode strings (use `strings.xml`)

3. **Compose Anti-Patterns**
   - ❌ Use Views/XML layouts (this is a Compose-only app)
   - ❌ Use deprecated Compose APIs
   - ❌ Perform IO operations in Composables
   - ❌ Create ViewModels inside Composables (use `hiltViewModel()`)

4. **Data Layer**
   - ❌ Use blocking calls on main thread
   - ❌ Expose entities directly (use mappers to domain models)
   - ❌ Skip error handling in network/database operations
   - ❌ Hardcode base URLs (use `BuildConfig` or config)

5. **Security**
   - ❌ Commit keystore files or secrets
   - ❌ Log sensitive data (passwords, tokens)
   - ❌ Store tokens in plain text (use EncryptedSharedPreferences)
   - ❌ Skip TLS certificate validation

6. **Version Control**
   - ❌ Add dependencies without updating `libs.versions.toml`
   - ❌ Commit generated files (`build/`, `.gradle/`, etc.)
   - ❌ Force push to main/master
   - ❌ Skip hooks (pre-commit, etc.)

### When Making Changes

1. **Read files first**: Always read relevant files before editing
2. **Understand context**: Check related files (e.g., repository, use case, ViewModel)
3. **Follow existing patterns**: Match the style of surrounding code
4. **Test changes**: Run builds and tests locally
5. **Update documentation**: If changing architecture or adding features

### Common Pitfalls

1. **Import Issues**
   - The `:app` module uses `src/main/java/` for Kotlin files
   - The `:data` and `:domain` modules use `src/main/kotlin/`
   - Watch for import conflicts between `Task` (domain) and `TaskEntity` (data)

2. **Compose State**
   - Always use `collectAsState()` for Flows in Composables
   - Use `remember { }` for state that should survive recomposition
   - Use `LaunchedEffect` for side effects (e.g., auto-refresh)

3. **Database Migrations**
   - Room schema changes require migrations in `DatabaseMigrations.kt`
   - Export schema to `data/schemas/` directory
   - Update version number in `NextcloudTasksDatabase`

4. **CalDAV Sync**
   - Always handle `Result<T>` from CalDAV service methods
   - Check for `null` account ID before syncing
   - Use ETags to avoid conflicts

---

## Common Tasks

### Adding a New Feature

1. **Create domain model** (if needed) in `:domain/model/`
2. **Add repository method** in `:domain/repository/` interface
3. **Implement repository** in `:data/repository/`
4. **Create use case** in `:domain/usecase/`
5. **Add UI** in `:app/` (ViewModel + Composable)
6. **Update Hilt modules** (if new dependencies)
7. **Write tests**
8. **Run quality checks**

### Adding a New Dependency

1. **Update** `gradle/libs.versions.toml`:
   ```toml
   [versions]
   newLib = "1.0.0"

   [libraries]
   new-lib = { module = "com.example:library", version.ref = "newLib" }
   ```

2. **Add to** module's `build.gradle.kts`:
   ```kotlin
   implementation(libs.new.lib)
   ```

3. **Sync** and verify build

### Updating SDK Versions

1. Update in **all three** `build.gradle.kts` files (app, data, domain)
2. Update `compileSdk`, `targetSdk` in `:app` and `:data`
3. Test thoroughly (especially Compose and Material 3)

### Adding a Database Field

1. **Update entity** in `:data/database/entity/`
2. **Create migration** in `:data/database/migrations/DatabaseMigrations.kt`
3. **Update version** in `NextcloudTasksDatabase`
4. **Export schema**: Set `room.schemaLocation` in KSP args
5. **Update mapper** in `:data/mapper/`
6. **Update domain model** (if needed)
7. **Test migration**

### Debugging CalDAV Issues

1. Enable debug build for logging
2. Check Logcat for `CalDavService` tag
3. Verify server URL format (must end with `/`)
4. Check authentication (credentials, tokens)
5. Inspect PROPFIND/REPORT responses
6. Validate VTODO format with iCal4j

---

## Quick Reference

### Essential Commands

```bash
# Format & analyze
./gradlew ktlintCheck detekt :app:lintDebug

# Build debug
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Build release
./gradlew bundleRelease

# Clean
./gradlew clean
```

### Key File Paths

```
app/build.gradle.kts                          # App module config
data/build.gradle.kts                         # Data module config
domain/build.gradle.kts                       # Domain module config
gradle/libs.versions.toml                     # Dependency versions
config/detekt/detekt.yml                      # Detekt rules
.github/workflows/ci.yml                      # CI/CD pipeline
fastlane/Fastfile                             # Fastlane config
app/src/main/java/com/nextcloud/tasks/MainActivity.kt  # Main UI
data/src/main/kotlin/com/nextcloud/tasks/data/repository/DefaultTasksRepository.kt  # Task repo
domain/src/main/kotlin/com/nextcloud/tasks/domain/model/Task.kt  # Task model
```

### Important Annotations

```kotlin
// Dependency Injection
@HiltAndroidApp              // Application class
@AndroidEntryPoint           // Activity/Fragment
@HiltViewModel              // ViewModel
@Inject constructor()       // Inject dependencies
@Singleton                  // Singleton scope
@Binds                      // Interface binding
@Provides                   // Factory method

// Room
@Database                   // Database class
@Dao                        // DAO interface
@Entity                     // Table entity
@Transaction                // Transactional query
@TypeConverter              // Type converter

// Compose
@Composable                 // Composable function
@Preview                    // Preview function
@OptIn                      // Opt-in experimental API

// Detekt
@Suppress("RuleName")       // Suppress specific rule
```

---

## Additional Resources

- **README.md**: User-facing documentation (German)
- **AGENTS.md**: Alternative AI agent guide (slightly outdated)
- **Kotlin Docs**: https://kotlinlang.org/docs/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Hilt**: https://dagger.dev/hilt/
- **Room**: https://developer.android.com/training/data-storage/room
- **CalDAV RFC**: https://tools.ietf.org/html/rfc4791
- **iCal4j**: https://www.ical4j.org/

---

**End of CLAUDE.md**

This document is maintained by AI assistants working on this codebase. When making significant architectural changes or adding major features, please update this document accordingly.
