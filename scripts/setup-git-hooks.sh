#!/bin/bash
# Setup Git hooks for Nextcloud Tasks Android
# Run this once after cloning the repository

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"
SCRIPTS_DIR="$REPO_ROOT/scripts"

echo "🔧 Setting up Git hooks for Nextcloud Tasks Android..."
echo ""

# Install pre-commit hook
echo "Installing pre-commit hook..."
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash
# Auto-generated pre-commit hook
# Runs quality checks before allowing commits

# Get repository root
REPO_ROOT="$(git rev-parse --show-toplevel)"

# Run pre-commit checks
"$REPO_ROOT/scripts/pre-commit-checks.sh"
EOF

chmod +x "$HOOKS_DIR/pre-commit"

echo "✅ Pre-commit hook installed at: .git/hooks/pre-commit"
echo ""
echo "🎉 Git hooks setup complete!"
echo ""
echo "The pre-commit hook will now run automatically on every commit."
echo "It performs:"
echo "  • ktlint code formatting check"
echo "  • detekt static analysis"
echo ""
echo "To bypass the hook (NOT recommended), use: git commit --no-verify"
echo ""
