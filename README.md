# Follow AI Auto 🎬🤖

Production-ready Android automation engine designed for bulk video generation on the **Follow AI** web platform inside mobile browsers (such as Kiwi Browser, Chrome, or Brave).

---

## 📁 Repository Structure

```text
├── .github/
│   └── workflows/
│       └── build-apk.yml             # GitHub Actions auto-compile & artifact release workflow
├── app/
│   ├── build.gradle.kts              # Application build config (Java 17, Compose BOM, AndroidX)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Permissions, Services, and Accessibility declarations
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # Dashboard, permissions setup, presets, and file picker
│       │   │   ├── controller/
│       │   │   │   └── AutomationEngine.kt     # Reactive queue orchestrator, history, and alerts
│       │   │   ├── model/
│       │   │   │   └── AutomationModels.kt     # Presets, sanitization, calibration, and state
│       │   │   ├── service/
│       │   │   │   ├── FloatingService.kt       # System Alert Window (bubble & control panel)
│       │   │   │   ├── FollowAIEngineService.kt # AccessibilityService & touch simulation
│       │   │   │   └── OverlayLifecycleOwner.kt # WindowManager Compose lifecycle host
│       │   │   └── ui/
│       │   │       ├── overlay/
│       │   │       │   └── OverlayContent.kt    # Floating UI, calibration wizard, circular badge
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       ├── mipmap-*/                    # Adaptive launcher icons across mdpi to xxxhdpi
│       │       ├── values/
│       │       │   └── strings.xml
│       │       └── xml/
│       │           ├── accessibility_service_config.xml
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/java/com/example/
│           ├── ExampleRobolectricTest.kt        # JVM unit & Robolectric tests for all modules
│           └── ExampleUnitTest.kt
├── build.gradle.kts                  # Root Gradle build file
├── gradle/
│   └── libs.versions.toml            # Version catalog
├── gradle.properties
├── metadata.json
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Setup & Permission Guide

To ensure smooth automation over mobile browsers, grant the required Android system permissions:

### 1. Enable Accessibility Permission
1. Open your phone's **Settings** ➡️ **Accessibility**.
2. Scroll to **Downloaded Apps / Services**.
3. Locate and tap **Follow AI Automation Engine** (or tap the direct shortcut button inside the app dashboard).
4. Toggle the switch to **ON** and accept the system prompt.
   > **Why?** This allows the app to locate the prompt input box, detect the "Generate" button, monitor modal popups (like Captcha or Login), and simulate realistic taps without requiring root.

### 2. Enable "Display over other apps" (Overlay Permission)
1. In the app dashboard, tap **"Grant Permission"** under *Display Over Other Apps*.
2. In the Android settings screen that appears, find **Follow AI Auto** and turn on **Allow display over other apps**.
   > **Why?** This displays the floating draggable bubble and queue control panel directly over Kiwi Browser or Chrome so you never have to switch back and forth.

### 3. Setting Up Browser Desktop Mode (Consistent Coordinate Mapping)
For the most reliable element detection and coordinate fallback:
1. Open **Kiwi Browser** or **Google Chrome** on your mobile device.
2. Navigate to `https://follow.ai` and sign into your account.
3. Open the browser menu (three dots in top-right) and check **"Desktop site"** (recommended for fixed-position toolbars).
4. Go to the Video Generation page.
5. Tap **"Launch Floating Overlay"** in Follow AI Auto. The floating bubble appears on your screen.
6. Tap the bubble to expand the control panel, paste your prompts or import a `.txt` / `.json` file, and press **Start Queue**!

---

## ⚡ Core Features

- **Bulk Prompt Importer & Sanitizer**: Import `.txt` or `.json` files; automatically strips blank lines and cleans unprintable characters.
- **Queue Progress & Resume**: If the browser refreshes or the phone restarts, the queue automatically remembers where it left off (e.g. Prompt #12 of 50).
- **Quick-Switch Mode Presets**:
  - `T2V`: Standard Text-to-Video generation.
  - `I2V`: Automatic Image-to-Video container tap sequence before prompt injection.
  - `Ratio Lock`: Automatically locks output ratio to **16:9 Landscape**, **9:16 Portrait**, or **1:1 Square**.
- **Interactive 4-Target Touch Calibration**: Tap anywhere on screen to record precise coordinates for Prompt Box, Model Selector, Generate Button, and Download Icon.
- **Smart Polling & Anti-Spam**: Checks if Generate is disabled or busy rendering before clicking.
- **Captcha / Login Auto-Pause**: Automatically pauses and displays an amber/red warning badge if verification or session modals appear.
- **Auto-Download & Completion Alerts**: Automatically clicks the video save icon after render, accompanied by completion haptics and notification sounds.
- **GitHub Actions Auto-Build**: Every commit or release tag automatically compiles and publishes the debug APK in the GitHub Actions tab.
