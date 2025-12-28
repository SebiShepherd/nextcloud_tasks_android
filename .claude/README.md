# Claude Code Configuration

This directory contains configuration and scripts for Claude Code AI assistant.

## Files

### `pre-commit-checks.sh`

**Mandatory pre-commit quality checks script.**

This script runs code quality validation using standalone tools that work in both:
- ✅ Claude Code sandbox environment (with network restrictions)
- ✅ Local development environment

#### What it checks:

1. **ktlint** (v1.4.1) - Kotlin code formatting
   - Validates code follows Kotlin style guide
   - Can auto-fix with: `/tmp/.nextcloud-tasks-tools/ktlint -F "**/*.kt"`

2. **detekt** (v1.23.8) - Static code analysis
   - Detects code smells, complexity issues, and anti-patterns
   - Uses project config: `config/detekt/detekt.yml`

#### Usage:

```bash
# Run before every commit
.claude/pre-commit-checks.sh
```

**Exit codes:**
- `0` - All checks passed ✅
- `1` - Checks failed (fix issues before committing) ❌

#### Why standalone tools?

Claude Code sandbox has Java network restrictions that prevent Gradle from downloading plugins and dependencies. This script uses standalone executables downloaded via `curl`, which works in the sandbox.

The tools are cached in `/tmp/.nextcloud-tasks-tools/` for faster subsequent runs.

#### Automatic downloads:

On first run, the script will:
1. Download ktlint binary from GitHub releases
2. Download and extract detekt-cli from GitHub releases
3. Cache them in `/tmp/.nextcloud-tasks-tools/`

Subsequent runs reuse the cached tools.

---

## Integration with CLAUDE.md

See `CLAUDE.md` section **"AI Assistant Guidelines"** for detailed instructions on when and how to use these tools.

**Key rule:** NEVER commit without running `.claude/pre-commit-checks.sh` first!
