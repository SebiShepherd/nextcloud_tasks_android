# Contributing to Nextcloud Tasks Android

Thank you for considering contributing to Nextcloud Tasks Android! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Translations](#translations)
- [Pull Requests](#pull-requests)

## Code of Conduct

Please be respectful and constructive in all interactions. We're building this together as a community.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/nextcloud_tasks_android.git
   cd nextcloud_tasks_android
   ```
3. **Set up the development environment**:
   - Android Studio (latest stable version recommended)
   - JDK 17
   - Android SDK with API levels 26-36

4. **Install Git hooks** (IMPORTANT - runs quality checks on every commit):
   ```bash
   ./scripts/setup-git-hooks.sh
   ```
   This installs a pre-commit hook that automatically runs ktlint and detekt before allowing commits.

5. **Build the project**:
   ```bash
   ./gradlew build
   ```

## Development Workflow

### Before Starting Work

1. Create a new branch from `main`:
   ```bash
   git checkout -b feature/my-feature
   ```

2. Keep your branch up to date:
   ```bash
   git pull origin main
   ```

### During Development

1. Follow the [Coding Standards](#coding-standards)
2. Write tests for new functionality
3. Run quality checks frequently:
   ```bash
   ./gradlew ktlintCheck detekt lintDebug
   ```

### Before Committing

**Quality checks run automatically** via Git pre-commit hook (if you ran `setup-git-hooks.sh`).

The hook will:
- ✅ Run ktlint formatting check
- ✅ Run detekt static analysis
- ❌ Block the commit if checks fail

If you want to run checks manually:
```bash
# Quick checks (runs automatically on commit)
./scripts/pre-commit-checks.sh

# Full checks (recommended before pushing)
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest
```

**Auto-fix formatting issues:**
```bash
./gradlew ktlintFormat
```

## Coding Standards

### Kotlin Style

- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for formatting (automatically checked in CI)
- Use detekt for static analysis

### Architecture

- Follow Clean Architecture principles (see `AGENTS.md`)
- Domain layer: Pure Kotlin, no Android dependencies
- Data layer: Repository implementations
- App layer: UI (Jetpack Compose) and ViewModels

### Dependency Injection

- Use Hilt for all dependency injection
- Annotate ViewModels with `@HiltViewModel`
- Use constructor injection where possible

### UI

- Use Jetpack Compose for all UI
- Follow Material 3 design guidelines
- Ensure accessibility (content descriptions, semantic properties)

## Translations

We welcome translations to make Nextcloud Tasks accessible to more users!

### Adding a New Language

1. **Create the strings file**:
   - Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-{language_code}/strings.xml`
   - Example: `values-fr/strings.xml` for French
   - Example: `values-es/strings.xml` for Spanish

2. **Translate all strings**:
   - Translate each `<string>` entry
   - Keep the `name` attribute unchanged
   - Maintain XML structure and formatting placeholders (e.g., `%1$s`, `%1$d`)

   Example:
   ```xml
   <!-- English (values/strings.xml) -->
   <string name="account_info">Account: %1$s</string>

   <!-- French (values-fr/strings.xml) -->
   <string name="account_info">Compte : %1$s</string>
   ```

3. **Update build configuration**:
   In `app/build.gradle.kts`, add your language code to `localeFilters`:
   ```kotlin
   androidResources {
       localeFilters += listOf("en", "de", "fr")  // Add "fr" for French
   }
   ```

   In `app/src/main/res/xml/locales_config.xml`, add the locale:
   ```xml
   <locale android:name="fr" />
   ```

4. **Add to Language enum**:
   In `app/src/main/java/com/nextcloud/tasks/preferences/LanguagePreferencesManager.kt`:
   ```kotlin
   enum class Language(val code: String, val displayName: String) {
       SYSTEM("", "System Default"),
       ENGLISH("en", "English"),
       GERMAN("de", "Deutsch"),
       FRENCH("fr", "Français"),  // Add new language
   ```

5. **Update language display names**:
   In `app/src/main/java/com/nextcloud/tasks/settings/SettingsScreen.kt`, update `getLanguageDisplayName()`:
   ```kotlin
   Language.FRENCH -> stringResource(R.string.language_french)
   ```

6. **Add string resources for language name**:
   Add entries in `values/strings.xml` and `values-de/strings.xml`:
   ```xml
   <string name="language_french">Français</string>
   ```

7. **Test your translation**:
   - Build and run the app
   - Go to Settings → Language
   - Select your new language
   - Navigate through all screens to verify translations

### Translation Guidelines

- **Be consistent**: Use the same terminology throughout the app
- **Keep it concise**: Mobile screen space is limited
- **Use native phrases**: Don't translate word-for-word; use natural expressions
- **Test formatting**: Ensure dynamic strings with parameters work correctly
- **Preserve placeholders**: `%1$s` (string), `%1$d` (number) must remain unchanged
- **Respect XML syntax**: Escape special characters (`'` → `\'`, `&` → `&amp;`)

### Updating Existing Translations

When English strings change:
1. Check all `values-{code}/strings.xml` files
2. Update the corresponding translations
3. Test that nothing is broken

### Translation Resources

- [Android String Resources Guide](https://developer.android.com/guide/topics/resources/string-resource)
- [String Formatting in Android](https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling)

## Pull Requests

### Before Submitting

1. Ensure all tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. Run code quality checks:
   ```bash
   ./gradlew ktlintCheck detekt lintDebug
   ```

3. Update documentation if needed (README.md, AGENTS.md)

### PR Guidelines

- **Title**: Use a clear, descriptive title
- **Description**: Explain what changes you made and why
- **Testing**: Describe how you tested your changes
- **Screenshots**: Include screenshots for UI changes
- **Translations**: If you added strings, confirm they're translated

### PR Template Example

```markdown
## Description
Brief description of changes

## Changes Made
- Added feature X
- Fixed bug Y
- Updated translations for Z

## Testing
- [ ] Built and ran on Android emulator
- [ ] Tested on physical device (API XX)
- [ ] All quality checks pass
- [ ] Translations verified

## Screenshots
(if applicable)
```

### Review Process

1. Automated checks will run (CI)
2. A maintainer will review your code
3. Address any feedback
4. Once approved, your PR will be merged

## Questions?

- Check `AGENTS.md` for technical details
- Open an issue for discussion
- Be patient - this is a community project

---

Thank you for contributing! 🎉
