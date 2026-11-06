#!/usr/bin/env bash
# ==============================================================================
# One-Shot Git Setup & Push Script for Termux / Linux Shell
# Automates Git initialization, staging, branch setup, and GitHub push.
# ==============================================================================

set -e

echo "🚀 [Follow AI Auto] Initializing Git push sequence..."

# 1. Check or prompt for GitHub Repository URL
REPO_URL="$1"
if [ -z "$REPO_URL" ]; then
    echo ""
    echo "Enter your target GitHub repository URL (HTTPS or SSH):"
    echo "Example: https://github.com/your-username/follow-ai-auto.git"
    read -r -p "Repository URL: " REPO_URL
fi

if [ -z "$REPO_URL" ]; then
    echo "❌ Error: Repository URL is required. Aborting."
    exit 1
fi

# 2. Configure Git identity if not already set
if [ -z "$(git config user.name)" ]; then
    git config user.name "FollowAI Auto Developer"
fi
if [ -z "$(git config user.email)" ]; then
    git config user.email "automator@followai.local"
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

# Only commit if there are changes
if git status --porcelain | grep -q .; then
    echo "💾 Committing changes..."
    git commit -m "feat: production Follow AI automation app with overlay, calibration, presets & CI/CD"
else
    echo "ℹ️ No uncommitted changes detected."
fi

# 7. Add or update remote origin
if git remote | grep -q "^origin$"; then
    echo "🔄 Updating existing remote origin to $REPO_URL..."
    git remote set-url origin "$REPO_URL"
else
    echo "🔗 Adding remote origin $REPO_URL..."
    git remote add origin "$REPO_URL"
fi

# 8. Push to GitHub
echo "⬆️ Pushing to GitHub (origin main)..."
git push -u origin main

echo ""
echo "=============================================================================="
echo "✅ Push successful! GitHub Actions is now building your Android APK."
echo "👉 Check build progress in your GitHub repo under the 'Actions' tab."
echo "📥 Download the compiled APK from the 'Artifacts' section once completed."
echo "=============================================================================="
