# Release Guide for Nextcloud Tasks Android

This document describes the complete release process for publishing new versions of the Nextcloud Tasks Android app.

---

## 📋 Overview

The app uses an automated release process based on **Git Tags**. When you create a version tag (e.g., `v1.0.0`), GitHub Actions automatically builds APK and AAB files and creates a GitHub Release with download links.

### What's Automated:
✅ APK build (signed, for direct download)
✅ AAB build (signed, for Google Play Store)
✅ Quality checks (ktlint, detekt, lint, tests)
✅ Automatic versioning
✅ GitHub Release creation with release notes
✅ Download URLs for APK and AAB

---

## 🔧 One-Time Setup in GitHub

### 1. Configure Branch Protection for `main`

**Steps:**
1. Go to your repository on GitHub
2. Click **Settings** → **Branches** (left sidebar)
3. Under "Branch protection rules" click **Add rule**
4. Configure the following settings:

   **Branch name pattern:**
   ```
   main
   ```

   **Protect matching branches - Enable these checkboxes:**

   - ✅ **Require a pull request before merging**
     - ✅ Require approvals: `1` (or more)
     - ✅ Dismiss stale pull request approvals when new commits are pushed
     - ✅ **Restrict who can dismiss pull request reviews**
       - Add yourself (and trusted maintainers) to the allowlist

   - ✅ **Require status checks to pass before merging**
     - ✅ Require branches to be up to date before merging
     - Select the following status checks (appear after first CI run):
       - `quality` (from .github/workflows/ci.yml)

   - ✅ **Require conversation resolution before merging**

   - ✅ **Do not allow bypassing the above settings**
     - Optional: Allow admins to bypass (for emergencies)
     - **Important**: Even if you're an admin, this means YOU also can't bypass the rules unless you explicitly check "Allow specified actors to bypass"

   - ✅ **Restrict who can push to matching branches**
     - **IMPORTANT FOR YOUR QUESTION**: This controls who can push directly (without PR)
     - Leave empty or add only yourself
     - **Note**: This does NOT control who can approve PRs - see note below

   - ✅ **Block force pushes** (very important!)

   - ✅ **Require linear history** (recommended)

5. Click **Create** or **Save changes**

#### 📝 Controlling PR Approvals

**Who can approve PRs?**

By default, anyone can comment on pull requests in public repositories, but repository maintainers can control who can formally approve them using GitHub's **Code review limits** feature.

**Restricting PR Approvals (Recommended):**

1. In the **same Branch protection rule**, scroll down to find:

   **"Code review limits"** section

2. ✅ Enable: **"Limit to users explicitly granted read or higher access"**

   This means:
   - Only users explicitly added as collaborators can approve PRs
   - Public contributors can submit PRs but **cannot approve** them
   - Everyone can still comment and participate in discussions

3. Managing approval permissions:
   - Go to **Settings** → **Collaborators and teams**
   - Add maintainers who should be able to approve PRs
   - Regular contributors don't need collaborator access to submit PRs
   - Result: **Only designated maintainers can approve and merge PRs**

**Recommended Setup:**
1. Enable "Require a pull request before merging" with 1+ approvals
2. Enable "Code review limits" → "Limit to users explicitly granted read or higher access"
3. Add trusted maintainers as collaborators with write/maintain access
4. Result: ✅ Only designated maintainers can approve and merge PRs

**Alternative Options:**

**Code Owners (GitHub Pro/Teams/Enterprise)**
- Create a `.github/CODEOWNERS` file to require specific reviewers
- Provides fine-grained control over who must approve changes to specific files
- Requires paid GitHub subscription

**Access Control Only**
- Control approvals by limiting who has write access to the repository
- More coarse-grained than "Code review limits"
- Works without additional configuration

---

### 2. Configure Repository Secrets

The release workflow requires signing secrets to build APK and AAB files.

**Go to:** Settings → Secrets and variables → Actions → Repository secrets

Required secrets:
- `SIGNING_KEYSTORE_BASE64` - Base64-encoded Android keystore
- `SIGNING_KEYSTORE_PASSWORD` - Keystore password
- `SIGNING_KEY_ALIAS` - Signing key alias
- `SIGNING_KEY_PASSWORD` - Signing key password

**📚 Don't have a keystore yet?** See **[SIGNING.md](SIGNING.md)** for a complete guide on:
- Creating an Android signing keystore
- Converting it to Base64
- Adding secrets to GitHub
- Security best practices

Optional (for Play Store publishing):
- `PLAY_SERVICE_ACCOUNT_JSON` - Service Account JSON for Play Store API

---

### 3. Configure Workflow Permissions

**Steps:**
1. Go to **Settings** → **Actions** → **General**
2. Scroll to "Workflow permissions"
3. Ensure **"Read and write permissions"** is enabled
4. ✅ Enable "Allow GitHub Actions to create and approve pull requests"

This is required for the release workflow to create GitHub Releases.

---

### 4. Make Repository Public

**⚠️ Important: Do this AFTER setting up branch protection!**

**Steps:**
1. Go to **Settings** (repository settings, not account settings)
2. Scroll down to **"Danger Zone"**
3. Click **"Change visibility"**
4. Select **"Make public"**
5. Confirm the action

**Pre-flight checklist:**
- ✅ Branch protection is active
- ✅ No secrets/credentials committed in code
- ✅ .gitignore is properly configured
- ✅ README.md is up to date and informative
- ✅ LICENSE file exists

---

## 🚀 Publishing a Release

### Step-by-Step Guide

#### 1. Prepare the Code

Ensure that:
- ✅ All desired features/fixes are merged
- ✅ The `main` branch is up to date
- ✅ All CI checks pass successfully

#### 2. Prepare Changelog (Optional)

Create a list of changes since the last release:
- New features
- Bug fixes
- Breaking changes
- Known issues

**Tip:** The release script automatically generates a changelog from git commits, but you can manually edit it afterwards.

#### 3. Create Version Tag

**Local Tags:**

```bash
# Checkout main branch
git checkout main
git pull origin main

# Create a tag (e.g., for version 1.0.0)
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to GitHub
git push origin v1.0.0
```

**Or via GitHub UI:**

1. Go to **Releases** → **Create a new release**
2. Click **"Choose a tag"** → **"Create new tag"**
3. Enter the tag name: `v1.0.0` (format: `v` + version number)
4. Select the `main` branch as target
5. **IMPORTANT:** Don't click "Publish" yet! The workflow will create the release automatically.

#### 4. Monitor the Release Workflow

1. Go to **Actions** in your repository
2. The "Release Build" workflow should start automatically
3. Monitor the progress (takes ~10-15 minutes):
   - Quality checks (ktlint, detekt, lint)
   - Unit tests
   - APK build
   - AAB build
   - Release creation

#### 5. Verify the Release

After successful workflow run:

1. Go to **Releases** in your repository
2. You should see a new release with tag `v1.0.0`
3. Verify the downloads:
   - ✅ `nextcloud-tasks-1.0.0.apk` - For direct download
   - ✅ `nextcloud-tasks-1.0.0.aab` - For Play Store upload
4. Check the release notes

#### 6. Edit Release Notes (Optional)

1. Click **Edit** on the release
2. Customize the automatically generated release notes:
   - Add highlights
   - Group changes (Features, Bug Fixes, etc.)
   - Add screenshots (if available)
   - Add breaking changes or migration notes
3. Click **Update release**

---

## 📝 Versioning Schema

The app uses **Semantic Versioning** (SemVer):

```
MAJOR.MINOR.PATCH

Example: 1.2.3
- MAJOR (1): Breaking changes / Major new features
- MINOR (2): New features (backwards compatible)
- PATCH (3): Bug fixes (backwards compatible)
```

**Version Code Calculation:**
```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

Examples:
1.0.0  → 10000
1.2.3  → 10203
2.0.0  → 20000
```

**Git Tag Format:**
- ✅ `v1.0.0` (with "v" prefix)
- ❌ `1.0.0` (without prefix)
- ❌ `release-1.0.0`

---

## 🔄 Workflow Details

### Release Workflow (`.github/workflows/release.yml`)

**Triggers:**
- On push of tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manually via "Run workflow" button

**Steps:**
1. **Checkout** - Check out code
2. **Setup** - JDK 17, Android SDK, Gradle cache
3. **Version Extraction** - Extract version from git tag
4. **Quality Checks** - ktlint, detekt, lint (release variant)
5. **Tests** - Run unit tests
6. **Build APK** - Build signed release APK
7. **Build AAB** - Build signed release bundle
8. **Rename** - Rename files with version number
9. **Release Notes** - Generate automatic release notes
10. **Create Release** - Create GitHub Release with APK and AAB

**Outputs:**
- GitHub Release with downloads
- Automatic release notes from git commits
- APK and AAB as release assets

---

## 🧪 Testing Before Release

### Local Testing

```bash
# Run quality checks locally
./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest

# Build signed APK locally (requires signing secrets as env variables)
export SIGNING_KEYSTORE_BASE64="..."
export SIGNING_KEYSTORE_PASSWORD="..."
export SIGNING_KEY_ALIAS="..."
export SIGNING_KEY_PASSWORD="..."
./gradlew assembleRelease

# APK will be at: app/build/outputs/apk/release/app-release.apk
```

### Test Release (Without Tag)

You can manually trigger the release workflow without creating a tag:

1. Go to **Actions** → **Release Build**
2. Click **"Run workflow"**
3. Select the branch (e.g., `main`)
4. Click **"Run workflow"**

This creates APK/AAB as **artifacts** (not as a release), downloadable for 30 days.

---

## 📱 APK Installation (For Users)

The APK can be installed directly on Android devices:

**Requirements:**
- Android 8.0 (API 26) or higher
- "Install from Unknown Sources" enabled

**Steps:**
1. Go to the GitHub Release
2. Download `nextcloud-tasks-X.X.X.apk`
3. Open the APK file on your Android device
4. Confirm installation

---

## 🎯 Google Play Store Publishing

If you want to publish the app on Google Play Store later:

1. **Upload AAB:**
   - Download `nextcloud-tasks-X.X.X.aab` from the release
   - Go to Google Play Console
   - Upload the AAB file

2. **Automatic Publishing (Optional):**
   - Configure `PLAY_SERVICE_ACCOUNT_JSON` secret
   - The `play-internal` job in `ci.yml` will automatically publish to Internal Track

---

## ❓ Troubleshooting

### Problem: Workflow fails at signing step

**Solution:** Verify that all signing secrets are correctly configured:
```bash
# Secrets must exist in GitHub Settings → Secrets:
SIGNING_KEYSTORE_BASE64
SIGNING_KEYSTORE_PASSWORD
SIGNING_KEY_ALIAS
SIGNING_KEY_PASSWORD
```

### Problem: Release is not created

**Solution:** Check workflow permissions:
- Settings → Actions → General → Workflow permissions
- Must be set to "Read and write permissions"

### Problem: Quality checks fail

**Solution:** Test locally before pushing tag:
```bash
./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest
```

Fix all errors and warnings, then push the tag.

### Problem: APK cannot be installed

**Possible causes:**
- Android version too old (min. Android 8.0 required)
- APK is not correctly signed (check signing config)
- Signature conflict with previous installation (uninstall first)

---

## 📚 Additional Resources

- **CI/CD Workflow:** `.github/workflows/ci.yml` - Runs on every push/PR
- **Release Workflow:** `.github/workflows/release.yml` - Runs on version tags
- **Build Config:** `app/build.gradle.kts` - Automatic versioning
- **Project Docs:** `CLAUDE.md` - Complete project documentation

---

**Questions or issues?** Open an issue on GitHub!
