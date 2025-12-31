# Git Hooks

This directory contains Git hooks that are tracked in the repository.

## Setup

After cloning the repository, run:

```bash
git config core.hooksPath .githooks
```

This is a one-time setup that tells Git to use hooks from this directory instead of `.git/hooks/`.

## Hooks

- **pre-commit**: Runs ktlint and detekt before allowing commits

## Bypass (Not Recommended)

To bypass hooks in an emergency:

```bash
git commit --no-verify
```
