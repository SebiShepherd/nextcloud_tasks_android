#!/bin/bash
# Pre-commit quality checks for Nextcloud Tasks Android
# This script runs ktlint and detekt before allowing commits
# Works in ALL environments: local IDE, CI/CD, and Claude Code

set -e

echo "🔍 Running pre-commit quality checks..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Detect if we're in a sandboxed environment (Gradle might not work)
GRADLE_WORKS=false

# Test if Gradle can run (has network access)
if ./gradlew --version &>/dev/null; then
    GRADLE_WORKS=true
    echo "ℹ️  Using Gradle for checks (local/CI environment)"
else
    echo "ℹ️  Gradle unavailable, using standalone tools (sandboxed environment)"
fi

echo ""

# Run checks with Gradle (preferred for local dev)
if [ "$GRADLE_WORKS" = true ]; then
    echo "1️⃣  Running ktlintCheck..."
    if ./gradlew ktlintCheck --quiet; then
        echo -e "${GREEN}✅ ktlint passed${NC}"
    else
        echo -e "${RED}❌ ktlint failed - please fix formatting issues${NC}"
        echo ""
        echo "To auto-fix, run: ./gradlew ktlintFormat"
        exit 1
    fi

    echo ""
    echo "2️⃣  Running detekt..."
    if ./gradlew detekt --quiet; then
        echo -e "${GREEN}✅ detekt passed${NC}"
    else
        echo -e "${RED}❌ detekt failed - code quality issues found${NC}"
        exit 1
    fi
else
    # Fallback: Use standalone tools (for Claude Code sandbox)
    KTLINT_VERSION="1.4.1"
    DETEKT_VERSION="1.23.8"
    TOOLS_DIR="/tmp/.nextcloud-tasks-tools"

    mkdir -p "$TOOLS_DIR"

    # Download ktlint if not present
    if [ ! -f "$TOOLS_DIR/ktlint" ]; then
        echo "📥 Downloading ktlint $KTLINT_VERSION..."
        curl -sSLo "$TOOLS_DIR/ktlint" "https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint"
        chmod +x "$TOOLS_DIR/ktlint"
    fi

    # Download detekt if not present
    if [ ! -d "$TOOLS_DIR/detekt-cli-${DETEKT_VERSION}" ]; then
        echo "📥 Downloading detekt $DETEKT_VERSION..."
        curl -sSL "https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}.zip" -o "$TOOLS_DIR/detekt.zip"
        unzip -q "$TOOLS_DIR/detekt.zip" -d "$TOOLS_DIR"
        rm "$TOOLS_DIR/detekt.zip"
    fi

    echo ""
    echo "1️⃣  Running ktlint..."
    if "$TOOLS_DIR/ktlint" "**/*.kt" --reporter=plain; then
        echo -e "${GREEN}✅ ktlint passed${NC}"
    else
        echo -e "${RED}❌ ktlint failed - please fix formatting issues${NC}"
        echo ""
        echo "To auto-fix, run: $TOOLS_DIR/ktlint -F \"**/*.kt\""
        exit 1
    fi

    echo ""
    echo "2️⃣  Running detekt..."

    # Create detekt config without formatting section (standalone doesn't support it)
    DETEKT_CONFIG="/tmp/detekt-config.yml"
    head -32 config/detekt/detekt.yml > "$DETEKT_CONFIG"

    if "$TOOLS_DIR/detekt-cli-${DETEKT_VERSION}/bin/detekt-cli" \
        --config "$DETEKT_CONFIG" \
        --input app/src,data/src,domain/src \
        --report "txt:/tmp/detekt-report.txt" 2>&1 | grep -q "Successfully"; then
        echo -e "${GREEN}✅ detekt passed${NC}"
    else
        echo -e "${RED}❌ detekt failed - code quality issues found${NC}"
        echo ""
        echo "Report saved to: /tmp/detekt-report.txt"
        if [ -f /tmp/detekt-report.txt ]; then
            cat /tmp/detekt-report.txt
        fi
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}✨ All quality checks passed!${NC}"
echo ""
