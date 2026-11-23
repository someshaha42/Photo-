# PixelForge Studio (Offline Android & Web Studio)

PixelForge is a 100% code-based, standalone, offline photo and text editor inspired by PixelLab. It operates entirely on native Kotlin/Jetpack Compose, Java compatibility fallbacks, and C++ native engines—with **zero runtime AI dependencies, zero generative models, and zero cloud API keys**.

---

## 📱 How to Import into Android Studio (অ্যান্ড্রয়েড স্টুডিওতে খোলার নির্দেশিকা)

You can import this project directly into standard desktop **Android Studio** (Koala, Ladybug, Meerkat, Otter, or 2024.1+) to develop, modify, and release updates locally on your computer.

### Step-by-Step Instructions:

1. **Download / Export Project**:
   - In AI Studio, click the settings or export menu to **Download ZIP** or **Push to GitHub**.
   - If downloaded as a ZIP, extract it to a folder on your computer (e.g., `D:\Projects\PixelForge`).

2. **Open in Android Studio**:
   - Launch **Android Studio**.
   - On the Welcome screen, click **Open** (or go to `File` → `Open...`).
   - Navigate to the extracted folder where `build.gradle.kts` and `settings.gradle.kts` are located.
   - Click **OK** to open the root directory.

3. **Automatic Gradle Sync**:
   - Android Studio will detect Gradle and the Version Catalog (`gradle/libs.versions.toml`).
   - Gradle will sync all dependencies automatically using Google Maven and Maven Central.
   - **Recommended JDK**: Java 17 or Java 21 (bundled by default in Android Studio via JetBrains Runtime `jbr-17` or `jbr-21`).

4. **Run the App**:
   - Connect your Android phone via USB (with USB Debugging enabled) or start an Android Virtual Device (AVD Emulator).
   - Click the green **Run (▶)** button in Android Studio.
   - The app will compile and install on your device with all tools, gestures, canvas layers, and export functions fully working!

---

## 🚀 How to Release Future Updates (ভবিষ্যতে আপডেট দেওয়ার নিয়ম)

Because this app is 100% standard Android code, you never need any AI tool to update or maintain it:

1. **Making Code Changes**:
   - UI Screens: `app/src/main/java/com/example/ui/screens/`
   - UI Components & Panels: `app/src/main/java/com/example/ui/components/`
   - Data & Storage: `app/src/main/java/com/example/data/`
   - C++ Native Processing: `app/src/main/cpp/`
   - Resources (Strings, Drawables, Icons): `app/src/main/res/`

2. **Incrementing Version for a New Update**:
   - Open `app/build.gradle.kts`.
   - Update `versionCode` (e.g., change `1` to `2`).
   - Update `versionName` (e.g., change `"1.0"` to `"1.1"`).

3. **Generating Signed APK / AAB for Release**:
   - In Android Studio top menu, click: `Build` → `Generate Signed Bundle / APK...`
   - Select **Android App Bundle** (for Google Play Store) or **APK** (for direct install/sharing).
   - Select your Keystore (`.jks`), enter your keystore password and alias.
   - Select `release` build variant and click **Finish**.
   - Android Studio will generate the signed `.aab` or `.apk` ready for publication on the Google Play Console!

---

## 🏗️ Project Architecture Breakdown

```
├── app/                          # Native Android Module
│   ├── build.gradle.kts          # App-level Gradle build configuration
│   ├── proguard-rules.pro        # ProGuard / R8 release obfuscation rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # Multi-device permissions & features
│           ├── cpp/              # C++ NDK Native Engine (ARM64, ARM32, x86_64, x86)
│           │   ├── CMakeLists.txt
│           │   ├── pixelforge_native.h
│           │   └── pixelforge_native.cpp
│           ├── java/com/example/
│           │   ├── MainActivity.kt
│           │   ├── compat/       # NDK JNI Bridge, Device Classifier & Java Compat
│           │   ├── data/         # Media storage, Room DB, project persistence
│           │   ├── model/        # Text & Image layer models
│           │   └── ui/           # Jetpack Compose screens, panels, and theme
│           └── res/              # Drawables, strings, icons, and XML configs
│
├── gradle/
│   ├── libs.versions.toml        # Gradle Version Catalog (Dependencies & Versions)
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── src/                          # Standalone Web Companion (HTML5/React/TypeScript)
│   ├── components/               # Modular UI panels & dialogs
│   ├── services/                 # Canvas engine, touch gesture & audio service
│   ├── styles/                   # CSS styling & tokens
│   └── assets/                   # Standalone PNG icons, images, audio & animations
│
├── build.gradle.kts              # Root Project Gradle configuration
├── settings.gradle.kts           # Root Gradle settings (includes :app)
├── gradle.properties             # JVM heap and compiler optimization flags
└── metadata.json                 # Project descriptor
```

---

## 🔒 100% Offline & Pure Source Code Guarantee
- **No Generative AI Prompts or LLM API Calls**: All image filters, corner pins, text formatting, and transformations are executed directly via mathematical algorithms on the device's CPU/GPU.
- **No Cloud Database Required**: Projects and layers are saved locally on device storage using SQLite/Room (Android) and LocalStorage (Web).
- **Physical Assets**: All 32 icons are physical standalone PNG assets (`/assets/icons/`), with no dynamic web-generated or placeholder graphics.
