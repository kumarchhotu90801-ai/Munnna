#!/usr/bin/env bash
# ==============================================================================
# One-Shot Git Setup & Push Script for Termux / Linux Shell
# Automates Git initialization, staging, branch setup, and GitHub push.
# ==============================================================================

set -e

REPO_OWNER="kumarchhotu90801-ai"
REPO_NAME="Munnna"
DEFAULT_REPO_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}.git"

echo "🚀 [Follow AI Auto] Initializing Git push sequence for ${REPO_OWNER}/${REPO_NAME}..."

# 1. Check if token or URL provided as argument
ARG1="$1"
GITHUB_TOKEN=""
REPO_URL=""

if [[ "$ARG1" =~ ^ghp_ ]] || [[ "$ARG1" =~ ^github_pat_ ]]; then
    GITHUB_TOKEN="$ARG1"
    REPO_URL="https://${GITHUB_TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git"
elif [ -n "$ARG1" ]; then
    REPO_URL="$ARG1"
fi

# If token is in GITHUB_TOKEN environment variable
if [ -z "$REPO_URL" ] && [ -n "$GITHUB_TOKEN_ENV" ]; then
    REPO_URL="https://${GITHUB_TOKEN_ENV}@github.com/${REPO_OWNER}/${REPO_NAME}.git"
fi

if [ -z "$REPO_URL" ]; then
    echo ""
    echo "Enter your GitHub Personal Access Token (PAT) with 'repo' scope:"
    echo "(Or press Enter to attempt standard push with cached credentials/SSH)"
    read -r -s -p "GitHub Token (hidden): " GITHUB_TOKEN
    echo ""
    if [ -n "$GITHUB_TOKEN" ]; then
        REPO_URL="https://${GITHUB_TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git"
    else
        REPO_URL="$DEFAULT_REPO_URL"
    fi
fi

# 2. Configure Git identity if not already set
if [ -z "$(git config user.name)" ]; then
    git config user.name "kumarchhotu90801-ai"
fi
if [ -z "$(git config user.email)" ]; then
    git config user.email "kumarchhotu90801@gmail.com"
fi

# 3. Initialize Git repository if needed
if [ ! -d ".git" ]; then
    echo "📦 Initializing local Git repository..."
    git init
fi

# 4. Ensure gradlew has executable permissions
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
fi

# 5. Set main branch
echo "🌿 Configuring 'main' branch..."
git branch -M main

# 6. Stage and commit all files
echo "📂 Staging project files..."
git add -A

# Only commit if there are uncommitted changes
if git status --porcelain | grep -q .; then
    echo "💾 Committing changes..."
    git commit -m "fix(app): resolve Kotlin compilation errors, missing imports, and align app dependencies"
else
    echo "ℹ️ No uncommitted changes detected. Working tree clean."
fi

# 7. Add or update remote origin
if git remote | grep -q "^origin$"; then
    echo "🔄 Updating existing remote origin..."
    git remote set-url origin "$REPO_URL"
else
    echo "🔗 Adding remote origin..."
    git remote add origin "$REPO_URL"
fi

# 8. Push to GitHub
echo "⬆️ Pushing to GitHub (origin main)..."
git push -u origin main

echo ""
echo "=============================================================================="
echo "✅ Push successful! GitHub Actions is now building your Android APK."
echo "👉 Check build progress in your GitHub repo: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions"
echo "📥 Download the compiled APK from the 'Artifacts' section once completed."
echo "=============================================================================="
