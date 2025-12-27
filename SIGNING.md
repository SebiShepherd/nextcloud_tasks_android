# Android Signing Setup Guide

This guide explains how to create an Android signing keystore and configure it for GitHub Actions.

---

## 📋 Overview

To publish signed APK/AAB files, you need:
1. **A keystore file** - Contains your signing key (keep this SECRET!)
2. **GitHub Secrets** - Secure storage for keystore and passwords

---

## 🔑 Step 1: Create Android Keystore

### Option A: Using Android Studio (Easiest)

1. Open your project in Android Studio
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **Android App Bundle** (or APK)
4. Click **Create new...** under "Key store path"
5. Fill in the form:
   - **Key store path**: Choose location (e.g., `~/nextcloud-tasks-release.jks`)
   - **Password**: Choose a strong password (save it!)
   - **Alias**: `nextcloud-tasks` (or your preferred name)
   - **Key password**: Choose a strong password (can be same as keystore password)
   - **Validity**: 25 years (or more)
   - **Certificate info**: Fill in your details (Name, Organization, etc.)
6. Click **OK** to create the keystore

### Option B: Using Command Line

```bash
# Generate keystore
keytool -genkey -v \
  -keystore ~/nextcloud-tasks-release.jks \
  -alias nextcloud-tasks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD

# You'll be prompted for certificate information:
# - Name, Organization, City, State, Country Code
# - Fill these in as appropriate
```

**Important Notes:**
- ⚠️ **Keep the keystore file safe!** If you lose it, you can't update your app on Play Store
- ⚠️ **Never commit the keystore to git!** (It's in `.gitignore` already)
- ⚠️ **Save all passwords!** You'll need them for GitHub Secrets
- 💡 Consider backing up the keystore to a secure location (password manager, encrypted backup)

---

## 📦 Step 2: Convert Keystore to Base64

GitHub Secrets can't store binary files directly, so we convert the keystore to Base64:

### On Linux/macOS:

```bash
# Convert keystore to base64
base64 -i ~/nextcloud-tasks-release.jks | tr -d '\n' > keystore-base64.txt

# Or in one command (output to clipboard on macOS):
base64 -i ~/nextcloud-tasks-release.jks | tr -d '\n' | pbcopy

# On Linux (with xclip):
base64 -i ~/nextcloud-tasks-release.jks | tr -d '\n' | xclip -selection clipboard
```

### On Windows (PowerShell):

```powershell
# Convert keystore to base64
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\nextcloud-tasks-release.jks")) | Set-Clipboard
```

The base64 string is now in your clipboard (or in `keystore-base64.txt`).

---

## 🔒 Step 3: Add Secrets to GitHub

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** for each of the following:

### Required Secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `SIGNING_KEYSTORE_BASE64` | The base64 string from Step 2 | `MIIJiAIBAzCCCU4GCSqGSIb3DQEHA...` (very long) |
| `SIGNING_KEYSTORE_PASSWORD` | Password you set for the keystore | `MySecurePassword123!` |
| `SIGNING_KEY_ALIAS` | Alias you set when creating keystore | `nextcloud-tasks` |
| `SIGNING_KEY_PASSWORD` | Password you set for the key | `MySecurePassword123!` (can be same) |

**Steps to add each secret:**
1. Click **New repository secret**
2. Enter the **Name** (e.g., `SIGNING_KEYSTORE_BASE64`)
3. Paste the **Secret** value
4. Click **Add secret**
5. Repeat for all 4 secrets

---

## ✅ Step 4: Verify Setup

After adding all secrets, you can test the setup:

### Option A: Create a Test Tag

```bash
# Create a test tag
git tag -a v0.0.1-test -m "Test release"
git push origin v0.0.1-test

# This will trigger the release workflow
# Check Actions tab to see if it succeeds
```

### Option B: Manual Workflow Run

1. Go to **Actions** → **Release Build**
2. Click **Run workflow**
3. Select branch: `main`
4. Click **Run workflow**
5. Wait for completion and check for errors

### What to Check:

- ✅ Workflow completes successfully
- ✅ APK and AAB are created and uploaded
- ✅ No signing errors in the logs
- ✅ APK installs correctly on an Android device

If you created a test tag (`v0.0.1-test`), you can delete it after testing:

```bash
# Delete local tag
git tag -d v0.0.1-test

# Delete remote tag
git push --delete origin v0.0.1-test

# Delete the GitHub Release (via GitHub UI)
# Go to Releases → Click on v0.0.1-test → Delete
```

---

## 🔐 Security Best Practices

1. **Never commit the keystore file**
   - Already in `.gitignore`
   - Double-check before committing

2. **Store passwords securely**
   - Use a password manager (1Password, Bitwarden, etc.)
   - Don't write them in plain text files

3. **Backup the keystore**
   - Store encrypted backup in secure cloud storage
   - Consider using multiple backup locations
   - You CANNOT recover this if lost!

4. **Limit access to secrets**
   - Only add trusted collaborators to the repository
   - Review who has access periodically

5. **Rotate secrets if compromised**
   - If keystore is compromised, create a new one
   - Note: Can't update existing Play Store app with new keystore
   - For new apps, use Play App Signing (Google manages keys)

---

## 🎯 Google Play App Signing (Recommended)

If you plan to publish on Google Play Store, consider using **Play App Signing**:

**Benefits:**
- Google manages the production signing key
- You can lose your upload key and recover
- More secure

**How it works:**
1. You sign APK/AAB with your upload key (the one you created above)
2. You upload to Play Console
3. Google re-signs with their production key before distribution
4. You can reset your upload key if needed

**To enable:**
1. Go to Play Console → Your App → Setup → App integrity
2. Follow instructions to enroll in Play App Signing
3. Upload your upload key (the one from this guide)

---

## ❓ Troubleshooting

### Problem: "Keystore was tampered with, or password was incorrect"

**Solution:**
- Double-check `SIGNING_KEYSTORE_PASSWORD` and `SIGNING_KEY_PASSWORD`
- Ensure base64 string is complete (no line breaks)
- Try converting keystore to base64 again

### Problem: "Key with alias 'xxx' does not exist"

**Solution:**
- Verify `SIGNING_KEY_ALIAS` matches the alias used when creating keystore
- List aliases in keystore: `keytool -list -v -keystore ~/nextcloud-tasks-release.jks`

### Problem: Base64 string seems too short or long

**Solution:**
- A typical keystore base64 string is 2000-5000 characters
- Ensure you used `tr -d '\n'` to remove line breaks
- Verify you're converting the correct file

### Problem: "Cannot run program 'git': No such file or directory"

**Solution:**
- This happens during local builds when trying to read version from git tag
- Set VERSION_NAME environment variable: `export VERSION_NAME=1.0.0`
- Or create a git tag: `git tag -a v1.0.0 -m "Version 1.0.0"`

---

## 📚 Additional Resources

- [Android Developer Guide: Sign your app](https://developer.android.com/studio/publish/app-signing)
- [Play Console: App signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

---

**Questions?** Open an issue on GitHub!
