# Scripts

Development and maintenance scripts for Nextcloud Tasks Android.

## Setup

### First-time setup

After cloning the repository, run:

```bash
./scripts/setup-git-hooks.sh
```

This installs Git hooks that enforce code quality before commits.

## Available Scripts

### `setup-git-hooks.sh`

Installs Git pre-commit hooks.

**What it does:**
- Installs `.git/hooks/pre-commit` that runs quality checks
- Ensures all commits pass ktlint and detekt validation

**When to run:**
- Once after cloning the repository
- After Git hooks are accidentally deleted
- When hooks are updated

**Usage:**
```bash
./scripts/setup-git-hooks.sh
```

---

### `pre-commit-checks.sh`

Runs code quality checks before allowing commits.

**What it checks:**
1. **ktlint** - Kotlin code formatting
2. **detekt** - Static code analysis

**Smart environment detection:**
- ✅ **Local IDE**: Uses `./gradlew ktlintCheck detekt` (preferred)
- ✅ **CI/CD**: Uses Gradle (if available)
- ✅ **Sandboxed environments** (Claude Code): Falls back to standalone tools

**Usage:**

Manual run (not needed if hooks are installed):
```bash
./scripts/pre-commit-checks.sh
```

Automatic run via Git hook:
```bash
git commit -m "Your message"
# Hook runs automatically before commit
```

**Exit codes:**
- `0` - All checks passed ✅
- `1` - Checks failed, commit blocked ❌

**Bypassing (NOT recommended):**
```bash
git commit --no-verify -m "Emergency fix"
```

---

## Maintenance

### Updating tool versions

To update ktlint or detekt versions, edit `pre-commit-checks.sh`:

```bash
KTLINT_VERSION="1.4.1"  # Update this
DETEKT_VERSION="1.23.8" # Update this
```

### Debugging failed checks

**ktlint failures:**
```bash
# See what's wrong
./gradlew ktlintCheck

# Auto-fix formatting
./gradlew ktlintFormat
```

**detekt failures:**
```bash
# See detailed report
./gradlew detekt

# View report
cat build/reports/detekt/detekt.txt
```

**Sandboxed environment:**
```bash
# Auto-fix ktlint
/tmp/.nextcloud-tasks-tools/ktlint -F "**/*.kt"

# View detekt report
cat /tmp/detekt-report.txt
```

---

## For AI Assistants

When working on this codebase:

1. **Always run pre-commit checks before committing:**
   ```bash
   ./scripts/pre-commit-checks.sh
   ```

2. **Never commit if checks fail**
   - Fix ktlint issues first
   - Fix detekt issues first
   - Only then proceed with commit

3. **The script works in all environments:**
   - Local development with Gradle
   - CI/CD pipelines
   - Sandboxed environments (Claude Code)

See `AGENTS.md` for detailed AI assistant guidelines.
