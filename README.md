# Kotoba Wall

Offline Japanese lock-screen wallpaper app built with Kotlin, Jetpack Compose, Material 3 and Google Material Icons.

## Features

- Choose a photo or one of four built-in gradients.
- 50 Japanese vocabulary entries with written form, kana reading and concise English meaning.
- Search words and categories; adjust size, vertical position, contrast panel and photo crop position.
- Render text into a static image, apply to the lock screen, or export PNG.
- Optional automatic rotation every 6 hours, 12 hours or daily using WorkManager.
- System dark mode and Android 12+ dynamic colors.
- No account, backend, analytics, accessibility service or floating overlay.

## Download a test APK

Open **Actions → Android build**. When a run succeeds, download the **KotobaWall-debug-apk** artifact, extract it, and install `app-debug.apk` on an Android 7.0+ device. You may need to allow installation from your browser/file manager.

**Source-generation status:** the project was not compiled or run on a device in the generation environment. Static checks passed, but compilation and actual device behavior require verification. A successful GitHub Actions run establishes build/test status, not complete device acceptance. See VALIDATION.md and docs/DEVICE_TEST_CHECKLIST.md.

## Build with VS Code

Prerequisites: **JDK 17**, **Android SDK platform 35**, **build-tools 35.0.0**, **platform-tools**, and **Python 3.9+**. Internet is required for the first build; the installed app works offline. Android Studio's SDK Manager is an easy way to install the SDK even if you edit in VS Code.

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to the SDK directory. Add platform-tools to PATH. Alternatively create `local.properties` with `sdk.dir=/your/sdk/path` (Windows accepts forward slashes). Do not commit this file.

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager --licenses
```

macOS/Linux:

```sh
chmod +x gradlew
./gradlew :app:assembleDebug
adb devices
./gradlew :app:installDebug
adb shell am start -n com.kotobawall.app/.MainActivity
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n com.kotobawall.app/.MainActivity
```

Enable USB debugging and accept the phone's authorization prompt, or start an emulator. APK output: `app/build/outputs/apk/debug/app-debug.apk`. VS Code build/install/test tasks are included. Use adb logcat or Android Studio for Android debugging; no pretend VS Code F5 debugger is configured.

### Gradle launcher

The included `gradlew` scripts are small Python-backed launchers, **not the official Gradle Wrapper**. They download Gradle 8.11.1, verify its SHA-256 against the upstream HTTPS checksum and cache it. Run only one first-time download at once. You can instead install Gradle 8.11.1 and run `gradle :app:assembleDebug` without Python.

To replace the launchers with a standard wrapper after Gradle setup:

```sh
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```

Commit generated wrapper files and pin the published distribution checksum. Never commit signing keys. This project uses AGP 8.9.2, Kotlin 2.1.20 and Compose BOM 2025.04.01; they are pinned, not claimed to be the latest. minSdk 24, compileSdk/targetSdk 35. Check current store target-API policies before publishing.

## Tests

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
# With a connected Android device/emulator:
./gradlew :app:connectedDebugAndroidTest
```

Unit tests cover crop math and cycling. Instrumented tests render every starter word at three positions. They do not prove pixel-perfect visuals or actual wallpaper behavior.

## Architecture

- `Models.kt`: data classes and crop/position math.
- `WallRepository.kt`: app-scoped repository, SharedPreferences, import/export and wallpaper operations. Coroutine mutex serializes updates; blocking work runs off the main thread.
- `WallpaperRenderer.kt`: sampled image decoding, EXIF correction, crop-to-fill and Japanese StaticLayout rendering. Always starts from the original image.
- `WallViewModel.kt`: editor state, debounced preview, busy state and messages.
- `MainActivity.kt`: Material 3 Studio, Words and Schedule screens.
- `RotationWorker.kt`: unique periodic work with limited transient I/O retries and opt-in checks.
- `assets/words.json`: editable starter word list.

## Behavior and limitations

- Editing the layout does not immediately replace the wallpaper; tap Apply or enable a schedule. Slider settings commit on release.
- Images are copied into app-private storage; imports are limited to 40 MB. JPEG/PNG are safest. Animated formats are treated as a static frame if supported by the decoder.
- The preview clock is a guide and is not burned into exported images. Android controls clocks, notifications and fingerprint indicators. Vocabulary is not tappable.
- Lock-screen only is requested, but cropping and OEM integration must be tested on your target phone. Output is portrait, capped within 1440 × 3200 while preserving screen aspect ratio. Tablets/foldables need extra testing.
- Text is center-aligned with vertical positioning; no free dragging, custom-font selector, pinch zoom or in-app custom-word editor. Edit words.json to add entries.
- Rotation uses the whole list, not the current library filter. WorkManager is approximate, not exact-time or screen-on scheduling. The first run is delayed by the chosen interval; battery restrictions and force-stop can postpone it.
- Automatic updates replace wallpapers selected outside this app. Turn scheduling off to keep external changes. An already executing write may finish just before disabling takes effect.
- The previous system wallpaper is not read or backed up. Clearing app data does not restore the old wallpaper.
- Definitions are concise sample data, not a complete dictionary. No JMdict dataset is bundled. If adding JMdict, comply with EDRDG attribution/share-alike terms and preserve reading/sense restrictions.
- No release signing setup is included. Configure your own keystore, store disclosures and current target SDK policy before distribution.

## Troubleshooting

Use JDK 17, not Java 25. Set ANDROID_HOME/local.properties for SDK errors. Add platform-tools to PATH for adb. For dependency failures, check access to services.gradle.org, distribution redirects, Google Maven and Maven Central. For image problems, try a smaller JPEG/PNG or a gradient. Device/work-profile policies can block wallpaper changes. Inspect crashes with `adb logcat -s AndroidRuntime`.

## References

- https://developer.android.com/reference/android/app/WallpaperManager
- https://developer.android.com/training/data-storage/shared/photo-picker
- https://developer.android.com/reference/android/text/StaticLayout.Builder
- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- https://developer.android.com/develop/ui/compose/designsystems/material3

See LICENSE and THIRD_PARTY_NOTICES.md.
