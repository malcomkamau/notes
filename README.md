# Notes

A secure, offline-first Android notes application built with modern Kotlin and Android Jetpack libraries.

## Features

- **User Authentication**: Secure Login and Sign-Up flows with custom input validation and password hashing (`PasswordHasher`, `SessionManager`).
- **Note Management**: Create, view, edit, and delete notes with a clean Material Design interface powered by `RecyclerView` and Fragments.
- **Security & Encryption**: Built-in cryptographic protection (`CryptoManager`) ensuring notes and data are securely handled.
- **Background Services & Receivers**:
  - `NoteBackupService`: Background service handling encrypted local backups.
  - `BootReceiver`: System broadcast receiver handling device reboots (`RECEIVE_BOOT_COMPLETED`).
- **Settings & Preferences**: Configurable app settings via `SettingsActivity` and `AppSettings`.
- **Modern UI/UX**: Bottom navigation (`MainActivity`, `NotesFragment`, `AccountFragment`) with Material Components and adaptive styling.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Compile/Target SDK**: 37 (Android 17)
- **UI Components**: Activities, Fragments, RecyclerView, ConstraintLayout, Material Design 3.
- **Persistence**: SQLite database via `DatabaseHelper`.
- **Security**: Android Security Cryptography (`CryptoManager`).

---

## Project Structure

```text
com.group14.notes/
├── auth/                 # Authentication logic (PasswordHasher, SessionManager, Validators)
├── data/                 # Database helper & Note models (DatabaseHelper, Note)
├── security/             # Cryptography & encryption (CryptoManager)
├── util/                 # Utilities (AppSettings, NotificationHelper, UiUtils)
├── AccountFragment.kt    # User account & settings fragment
├── BootReceiver.kt       # System boot broadcast receiver
├── LoginActivity.kt      # User login screen
├── MainActivity.kt       # Main container activity with bottom navigation
├── NoteBackupService.kt  # Background backup service
├── NoteEditorActivity.kt # Note creation and editing screen
├── NotesAdapter.kt       # RecyclerView adapter for notes list
├── NotesApp.kt           # Application class
├── NotesFragment.kt      # Notes list fragment
├── SettingsActivity.kt   # Settings screen
└── SignUpActivity.kt     # User registration screen
```

---

## Getting Started

1. **Prerequisites**:
   - Android Studio (Koala or newer recommended)
   - Android SDK with API 37 support
2. **Setup**:
   - Clone the repository.
   - Open the project folder in Android Studio.
3. **Build & Run**:
   - Sync project with Gradle files.
   - Run the app on an Android Emulator or physical device (API 26+).
