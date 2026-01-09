# Nextcloud Tasks Android

[![Android CI](https://github.com/SebiShepherd/nextcloud_tasks_android/actions/workflows/ci.yml/badge.svg)](https://github.com/SebiShepherd/nextcloud_tasks_android/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/SebiShepherd/nextcloud_tasks_android/branch/main/graph/badge.svg)](https://codecov.io/gh/SebiShepherd/nextcloud_tasks_android)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://android.com)

A modern, native Android client for Nextcloud Tasks built with Jetpack Compose and Clean Architecture principles.

---

## 📱 About

**Nextcloud Tasks Android** is a free and open-source Android application that seamlessly integrates with your Nextcloud server to manage tasks and to-do lists. Built from the ground up with modern Android development practices, the app offers a smooth, native experience while keeping your data under your control.

---

## ✨ Features

### 🔐 **Multi-Account Support**
- Connect multiple Nextcloud accounts simultaneously
- Easy account switching via the drawer menu
- Each account maintains its own task lists and settings
- Secure credential storage with encrypted preferences

### 📋 **Task Management**
- Create, edit, and delete tasks with a clean, intuitive interface
- Organize tasks into color-coded lists
- Mark tasks as completed with a single tap
- Full support for CalDAV VTODO standard
- Offline-first architecture - work without internet connection

### 🔄 **Seamless Synchronization**
- Real-time sync with your Nextcloud server via CalDAV
- Pull-to-refresh for manual sync
- Background synchronization (coming soon)
- Optimistic locking with ETags to prevent conflicts
- Automatic conflict resolution

### 🎨 **Modern Material Design**
- Material 3 design language throughout the app
- Dynamic color theming (Android 12+)
- Light and dark theme support
- Adaptive layouts for tablets and large screens
- Smooth animations and transitions

### 🌍 **Internationalization**
- Multi-language support with in-app language switcher
- Currently supported languages:
  - **English** (default)
  - **German** (Deutsch)
- Runtime language switching without app restart
- Uses Android 13+ per-app language preferences (with backport for Android 8-12)

### 🔒 **Privacy & Security**
- All data stays on your Nextcloud server - no third-party tracking
- Secure HTTPS communication with TLS certificate validation
- Support for password-based and OAuth 2.0 authentication
- No analytics, no ads, no data collection
- Open-source and transparent

---

## 📦 Installation

### Requirements
- **Android 8.0 (API 26)** or higher
- A **Nextcloud server** with the Tasks app installed

### Download Options

#### 1. **GitHub Releases** (Recommended for now)
Download the latest APK directly from the [Releases page](https://github.com/SebiShepherd/nextcloud_tasks_android/releases):

1. Go to [Releases](https://github.com/SebiShepherd/nextcloud_tasks_android/releases)
2. Download the latest `nextcloud-tasks-X.X.X.apk`
3. Enable "Install from Unknown Sources" in Android settings
4. Open the downloaded APK to install

#### 2. **Google Play Store** (Coming Soon)
The app will be available on Google Play Store soon.

#### 3. **Build from Source**
See the [CONTRIBUTING.md](CONTRIBUTING.md) guide for instructions on building the app yourself.

---

## 🚀 Getting Started

### First Launch

1. **Launch the app** - You'll be greeted with the login screen
2. **Enter your Nextcloud server URL** (e.g., `https://cloud.example.com`)
3. **Choose authentication method**:
   - **Password**: Enter your username and password
   - **OAuth 2.0**: Log in via your browser (more secure)
4. **Grant permissions** if prompted
5. **Start managing your tasks!**

### Adding Multiple Accounts

1. Open the **drawer menu** (☰ top-left)
2. Tap the **account selector** at the top
3. Tap **"Add Account"**
4. Follow the login flow for the new account
5. Switch between accounts anytime via the drawer menu

### Changing Language

1. Open the **drawer menu** (☰)
2. Go to **Settings**
3. Tap **Language**
4. Select your preferred language:
   - System default
   - English
   - Deutsch (German)

---

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture (3-layer: app, data, domain)
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **CalDAV**: iCal4j for VTODO parsing/generation
- **Async**: Kotlin Coroutines + Flow
- **Build System**: Gradle with Kotlin DSL

---

## 📚 Documentation

For detailed information about the project, development, and contributing, please refer to:

- **[RELEASE.md](RELEASE.md)** - Release process and versioning
- **[SIGNING.md](SIGNING.md)** - Android app signing setup guide
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines (coming soon)
- **[AGENTS.md](AGENTS.md)** - Comprehensive documentation for AI assistants and developers

---

## 🤝 Contributing

Contributions are welcome! Whether it's bug reports, feature requests, translations, or code contributions, we appreciate your help.

### Ways to Contribute

- **Report bugs**: Open an issue on GitHub
- **Suggest features**: Share your ideas in the issue tracker
- **Translate**: Add support for new languages (see `CONTRIBUTING.md`)
- **Code**: Submit pull requests (see development guidelines in `AGENTS.md`)

### Development Setup

See [AGENTS.md](AGENTS.md) for:
- Complete architecture overview
- Code conventions and patterns
- Development workflows
- Testing guidelines
- Common tasks and examples

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **Nextcloud**: https://nextcloud.com
- **Nextcloud Tasks App**: https://apps.nextcloud.com/apps/tasks
- **Report Issues**: https://github.com/SebiShepherd/nextcloud_tasks_android/issues

---

## 🙏 Acknowledgments

Built with ❤️ for the Nextcloud community.

Special thanks to:
- The Nextcloud team for creating an amazing platform
- The Android development community for excellent libraries and tools
- All contributors who help improve this app

---

**Enjoy managing your tasks with Nextcloud! 🎉**
