# Release Guide for Nextcloud Tasks Android

Quick reference for releasing new versions of the app.

---

## 📋 Overview

The app uses an automated release process based on **Git Tags**. When you push a version tag (e.g., `v1.0.0`), GitHub Actions automatically builds APK and AAB files and creates a GitHub Release.

---

## 🔧 One-Time GitHub Setup

### 1. Configure Branch Protection for `main`

**Steps:**
1. Go to **Settings** → **Branches**
2. Click **Add rule** under "Branch protection rules"
3. Set branch name pattern: `main`
4. Enable these options:

   **Required:**
   - ✅ Require a pull request before merging (with 1+ approvals)
   - ✅ Require status checks to pass before merging
     - Select: `quality` (appears after first CI run)
   - ✅ Require conversation resolution before merging
   - ✅ Block force pushes
   - ✅ Do not allow bypassing the above settings

   **Recommended:**
   - ✅ Require linear history
   - ✅ Restrict who can push to matching branches

5. Click **Create** or **Save changes**

### 2. Verify Repository Secrets

Go to **Settings** → **Secrets and variables** → **Actions**

Required secrets (should already exist):
- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_KEYSTORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

### 3. Configure Workflow Permissions

1. Go to **Settings** → **Actions** → **General**
2. Under "Workflow permissions", select **"Read and write permissions"**
3. ✅ Enable "Allow GitHub Actions to create and approve pull requests"

### 4. Make Repository Public

**⚠️ Important: Do this AFTER setting up branch protection!**

1. Go to **Settings** → scroll to **"Danger Zone"**
2. Click **"Change visibility"** → **"Make public"**
3. Confirm the action

**Pre-flight checklist:**
- ✅ Branch protection is active
- ✅ No secrets/credentials in code
- ✅ .gitignore is properly configured
- ✅ README.md is up to date
- ✅ LICENSE file exists

---

## 🚀 Creating a Release

### Step-by-Step

1. **Prepare the code**
   - Ensure all changes are merged to `main`
   - All CI checks pass

2. **Create and push a version tag**
   ```bash
   git checkout main
   git pull origin main

   # Create tag (e.g., for version 1.0.0)
   git tag -a v1.0.0 -m "Release version 1.0.0"

   # Push tag to GitHub
   git push origin v1.0.0
   ```

3. **Monitor the workflow**
   - Go to **Actions** tab
   - Watch the "Release Build" workflow (takes ~10-15 minutes)

4. **Verify the release**
   - Go to **Releases** tab
   - Download and test the APK
   - Optionally edit release notes

---

## 📝 Versioning

**Format:** Semantic Versioning (SemVer)
```
v<MAJOR>.<MINOR>.<PATCH>

Examples:
v1.0.0  - Initial release
v1.1.0  - New features
v1.1.1  - Bug fixes
v2.0.0  - Breaking changes
```

**Version Code Calculation:**
```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

Examples:
v1.0.0  → 10000
v1.2.3  → 10203
v2.0.0  → 20000
```

---

## 🧪 Testing Before Release

**Run quality checks locally:**
```bash
./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest
```

**Build signed APK locally:**
```bash
export SIGNING_KEYSTORE_BASE64="..."
export SIGNING_KEYSTORE_PASSWORD="..."
export SIGNING_KEY_ALIAS="..."
export SIGNING_KEY_PASSWORD="..."
./gradlew assembleRelease
```

**Manual workflow trigger (for testing):**
1. Go to **Actions** → **Release Build**
2. Click **"Run workflow"**
3. Select branch and run
4. Download artifacts (valid for 30 days)

---

## ❓ Troubleshooting

**Workflow fails at signing:**
- Verify all signing secrets are correctly set in GitHub Settings → Secrets

**Release not created:**
- Check Settings → Actions → General → Workflow permissions
- Must be "Read and write permissions"

**Quality checks fail:**
- Test locally first: `./gradlew ktlintCheck detekt :app:lintRelease testReleaseUnitTest`
- Fix all errors and warnings before pushing tag

---

## 📚 Additional Resources

- **German detailed guide:** `RELEASE.md`
- **CI/CD Workflow:** `.github/workflows/ci.yml`
- **Release Workflow:** `.github/workflows/release.yml`
- **Build Config:** `app/build.gradle.kts`
- **Project Documentation:** `CLAUDE.md`

---

**Questions?** Open an issue on GitHub!
