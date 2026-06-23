# AI Agent Guide - Nextcloud Tasks Android

**Last Updated**: 2026-06-24
**Project Version**: 1.0.0
**Target Audience**: Claude Code, AI assistants, and automated agents working on this codebase

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
- Offline-first architecture with Room database and pending operations queue
- Periodic background synchronization via WorkManager (every 15 minutes)
- Field-level merge conflict resolution (preserves local changes to different fields)
- Adaptive layouts for tablets and large screens (permanent navigation drawer, constrained content width)
- Pull-to-refresh synchronization
- Task lists with color coding
- Task filtering and sorting
- Network connectivity monitoring with validation grace period
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
  - `MainActivity.kt` - Entry point with all UI composables (adaptive layout support)
  - `TasksApp.kt` - Application class with `@HiltAndroidApp`
  - `TaskListViewModel.kt` - Task list state management and business logic
  - `auth/LoginScreen.kt`, `auth/LoginViewModel.kt` - Authentication UI
  - `sync/SyncScheduler.kt` - Periodic background sync scheduling via WorkManager
  - `sync/SyncWorker.kt` - Background sync worker implementation
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
  - `network/` - OkHttp clients, interceptors, SafeDns, NetworkMonitor
  - `sync/` - SyncManager, TaskFieldMerger (field-level merge), PendingOperationsManager
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
| **Room** | 2.7.2 | Local database |
| **Retrofit** | 2.11.0 | REST API client |
| **OkHttp** | 4.12.0 | HTTP client |
| **Coroutines** | 1.10.2 | Async programming |
| **iCal4j** | 3.2.18 | iCalendar VTODO parsing/generation |
| **Timber** | 5.0.1 | Logging |
| **Coil** | 2.7.0 | Image loading (avatars) |

### Build Tools

- **Android Gradle Plugin**: 9.0.0
- **Gradle**: 9.1.0 (wrapper)
- **KSP**: 2.1.10-1.0.29 (for Room)
- **Kapt**: Used for Hilt
- **ktlint**: 14.0.1 (code formatting)
- **detekt**: 1.23.8 (static analysis)

> **Daemon JVM**: The Gradle daemon is pinned to **JDK 17** via `gradle/gradle-daemon-jvm.properties`.
> This is required because detekt 1.23.x bundles an older IntelliJ/Kotlin compiler that crashes when it
> runs on newer JDKs (e.g. Java 25). If no JDK 17 is installed locally, Gradle auto-provisions one via the
> foojay toolchain resolver. Do not remove this file unless detekt is upgraded to a JDK-25-compatible release.

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
├── TaskListViewModel.kt         # Task list state, filtering, sorting, sync errors
├── auth/
│   ├── LoginScreen.kt           # Login UI
│   ├── LoginViewModel.kt        # Login business logic
│   └── LoginCallbacks.kt        # Login event callbacks
├── sync/
│   ├── SyncScheduler.kt         # Periodic background sync (WorkManager)
│   └── SyncWorker.kt            # Background sync worker
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
│   ├── NetworkMonitor.kt          # Connectivity monitoring with validation grace period
│   └── SafeDns.kt
├── sync/
│   ├── SyncManager.kt
│   ├── TaskFieldMerger.kt         # Field-level 3-way merge for sync conflicts
│   └── PendingOperationsManager.kt # Offline operations queue
├── auth/
│   ├── AuthToken.kt
│   └── AuthTokenProvider.kt     # Token management
├── mapper/
│   ├── TaskMapper.kt            # Entity ↔ Domain
│   ├── TaskListMapper.kt
│   └── TagMapper.kt
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

### Code Quality Checks

**🔧 Setup (run once after cloning):**

```bash
./scripts/setup-git-hooks.sh
```

This installs a Git pre-commit hook that **automatically** runs quality checks before every commit.

**Manual execution (if needed):**

```bash
# Run pre-commit checks manually
./scripts/pre-commit-checks.sh
```

The script **auto-detects** your environment:
- 🏠 **Local IDE with Gradle**: Runs `./gradlew ktlintCheck detekt` (fast)
- 🤖 **Claude Code / Sandboxed**: Uses standalone tools (downloads if needed)
- ⚙️ **CI/CD**: Uses Gradle

**For local development with full Gradle access:**

```bash
# Format check
./gradlew ktlintCheck

# Auto-fix formatting
./gradlew ktlintFormat

# Static analysis
./gradlew detekt

# Android lint
./gradlew :app:lintDebug

# Unit tests
./gradlew testDebugUnitTest

# All quality checks together
./gradlew ktlintCheck detekt :app:lintDebug testDebugUnitTest
```

**Environment-specific notes:**

- **Local IDE**: Git hook runs Gradle-based checks (preferred)
- **Claude Code**: Git hook uses standalone tools (Gradle blocked by sandbox)
- **CI/CD**: GitHub Actions runs full Gradle pipeline

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
app/src/test/java/        # App module unit tests (uses 'java' dir for Kotlin)
data/src/test/kotlin/     # Data module unit tests
domain/src/test/kotlin/   # Domain module unit tests (future)
```

### Existing Test Files

| Test File | Module | Covers |
|-----------|--------|--------|
| `TaskListViewModelTest.kt` | `:app` | ViewModel state, filtering, sorting, refresh errors, account switching |
| `TaskFieldMergerTest.kt` | `:data` | 3-way field-level merge, snapshot creation/parsing, conflict resolution |
| `DatabaseMigrationsTest.kt` | `:data` | Migration version ranges, contiguity, completeness |
| `DefaultTasksRepositoryMergeTest.kt` | `:data` | Repository clearAccountData, observeIsOnline, refresh skipping |
| `DefaultTasksRepositoryOfflineTest.kt` | `:data` | Offline-first pending operations |

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

- Use `kotlin("test")` framework with JUnit 4 (`@Test`, `@Before`, `@After`)
- Use `io.mockk` for mocking (coEvery, coVerify, every, mockk)
- Test ViewModels with `kotlinx-coroutines-test` (`UnconfinedTestDispatcher`, `runTest`)
- Set `Dispatchers.setMain()` in `@Before`, reset in `@After` for ViewModel tests
- Use helper functions (e.g., `withViewModel`, `withViewModelAndRepo`) to reduce test boilerplate
- Test Composables with `androidx.compose.ui.test`

### Test File Naming

- Test file: `TasksRepositoryTest.kt`
- Test class: `class TasksRepositoryTest { ... }`
- Test function: `` fun `should create task when draft is valid`() { ... } ``

---

## AI Assistant Guidelines

### ✅ DO's

### 🚨 MANDATORY Pre-Commit Checks (CRITICAL - NEVER SKIP!)

**Git hooks are automatically enabled** - checks run on every `git commit`.

If you need to run manually:

```bash
./scripts/pre-commit-checks.sh
```

**What it checks:**
- ✅ **ktlint** - Code formatting validation
- ✅ **detekt** - Static code analysis

**The script MUST pass with zero errors:**
- If ktlint fails: Fix formatting or run `./gradlew ktlintFormat`
- If detekt fails: Fix code quality issues reported
- **NEVER commit if these checks fail** (unless using `--no-verify`)

**Smart environment detection:**
- 🏠 **Local IDE**: Uses `./gradlew ktlintCheck detekt` (preferred, fastest)
- 🤖 **Claude Code**: Uses standalone tools (Gradle blocked by sandbox)
- ⚙️ **CI/CD**: Uses Gradle (standard pipeline)

**Why this is mandatory:**
- Ensures consistent code style across ALL developers
- Catches code quality issues before they reach CI/CD
- Prevents failed GitHub Actions runs
- Maintains high code quality standards
- **Not Claude-specific** - enforced for everyone via Git hooks

---

1. **Follow Clean Architecture**
   - Never add Android dependencies to `:domain`
   - Keep data sources in `:data`, business logic in `:domain`, UI in `:app`
   - Use interfaces in domain, implementations in data

2. **Use Dependency Injection**
   - Always use Hilt for DI
   - Use `@Inject constructor()` for repositories and use cases
   - Add `@HiltViewModel` to ViewModels
   - Add `@AndroidEntryPoint` to Activities

3. **Code Quality** (See MANDATORY Pre-Commit Checks above)
   - Git pre-commit hook runs automatically (installed via `scripts/setup-git-hooks.sh`)
   - Fix all warnings and errors before committing
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
4. **Quality checks run automatically**: Git pre-commit hook enforces ktlint + detekt
5. **Test changes**: Full builds validated by CI/CD (Gradle builds not possible in sandbox)
6. **Update documentation**: If changing architecture or adding features

**Workflow for all developers (including AI assistants):**
```bash
# 1. Make code changes

# 2. Commit (quality checks run automatically via Git hook)
git add .
git commit -m "Your message"
# → Pre-commit hook runs ./scripts/pre-commit-checks.sh
# → Commit succeeds only if checks pass

# 3. Push to remote
git push -u origin <branch-name>

# 4. CI/CD validates full build
# GitHub Actions will run: build, lint, tests
```

**Note:** The pre-commit hook can be bypassed with `git commit --no-verify`, but this is **strongly discouraged**.

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
   - Add new migrations to `DatabaseMigrations.all` array and update tests
   - Export schema to `data/schemas/` directory
   - Update version number in `NextcloudTasksDatabase`
   - Current schema: v6 (migrations: 1→2 CalDAV fields, 2→3 parent_uid, 3→4 account_id, 4→5 pending_operations table, 5→6 base_snapshot column)

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

- **README.md**: User-facing documentation
- **Kotlin Docs**: https://kotlinlang.org/docs/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Hilt**: https://dagger.dev/hilt/
- **Room**: https://developer.android.com/training/data-storage/room
- **CalDAV RFC**: https://tools.ietf.org/html/rfc4791
- **iCal4j**: https://www.ical4j.org/

---

**End of AGENTS.md**

This document is maintained by AI assistants working on this codebase. When making significant architectural changes or adding major features, please update this document accordingly.
