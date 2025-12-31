#!/bin/bash
# Setup Git hooks for Nextcloud Tasks Android
# Run this once after cloning the repository

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"

echo "🔧 Setting up Git hooks for Nextcloud Tasks Android..."
echo ""

# Configure Git to use tracked hooks from .githooks/
echo "Configuring Git to use hooks from .githooks/ directory..."
git config core.hooksPath .githooks

echo "✅ Git hooks configured successfully!"
echo ""
echo "🎉 Setup complete!"
echo ""
echo "Git is now configured to use hooks from the .githooks/ directory."
echo "These hooks are tracked in Git, so they work across all environments."
echo ""
echo "The pre-commit hook will run automatically on every commit."
echo "It performs:"
echo "  • ktlint code formatting check"
echo "  • detekt static analysis"
echo ""
echo "To bypass the hook (NOT recommended), use: git commit --no-verify"
echo ""
