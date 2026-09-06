# Kotoba Wall

Kotlin / Jetpack Compose / Material 3 Android app that renders Japanese vocabulary onto a static background and sets the result as lock-screen wallpaper.

## Version 1.3

- Dark panel defaults to 0%; old 40% settings migrate to 0, other saved values remain.
- Horizontal crop slider removed; vertical crop and Center photo remain.
- Bundled Gothic JP and Mincho JP Japanese fonts, each with real regular/bold files.
- Two/three-line designer: choose Japanese word, kana, meaning, combinations or custom template text. Per-line size, font, bold, color and alignment; position presets and live sliders.
- Words: optional public API downloads for N5–N1, multi-level selection, offline cache, favorites, favorites-only and shuffle. Level/favorite filters govern wallpaper rotation; text search only filters browsing.
- Photo Picker or four built-in gradients. Original photo stays in private storage; PNG export uses Android's document picker.
- Pinned live preview plus expanded preview. Save line layout before automatic updates/export, or use Save & apply.
- Opt-in screen-off service with ongoing notification/Stop; alternatively WorkManager every 6/12/24 hours. Both are best-effort under Android/OEM background restrictions. Never guaranteed on every wake.
- No account or analytics. Photos/settings are not uploaded. Vocabulary downloads disclose requested level and normal network metadata to the provider. Rotation uses cached words and does not fetch on screen-off.

See [version 1.3 notes](docs/V1_3.md), [screen-off limitations](docs/V1_1.md) and [third-party notices](THIRD_PARTY_NOTICES.md).

**Validation:** source checks passed in generation; Android build, unit tests/lint, device tests and actual UI/font appearance were not verified there. The existing GitHub Actions workflow builds the APK and runs unit tests/lint. Check its result before installing. Device tests/visual inspection are separate.

## Build in VS Code

VS Code is an editor, not an Android SDK. Install JDK **17**, Android SDK command-line tools, platform-tools, platform 35 and build-tools 35.0.0. Python 3.9+ is needed for the provided convenience Gradle launcher. First builds require internet for Gradle, Maven dependencies and bundled fonts.

Pinned toolchain: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, Compose BOM 2025.04.01. minSdk 24 (Android 7), compile/target SDK 35. Check current store target requirements before publishing. Do not use Java 25 with this pinned Gradle setup.

Open the folder containing settings.gradle.kts. Set JAVA_HOME to JDK 17 and ANDROID_HOME to your SDK, then add platform-tools to PATH. Alternatively add an uncommitted local.properties containing your SDK path, e.g. `sdk.dir=/Users/YOUR_NAME/Library/Android/sdk` (macOS) or `sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk` (Windows).

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager --licenses
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb devices
./gradlew :app:installDebug
adb shell am start -n com.kotobawall.app/.MainActivity
```

Windows PowerShell: use `.\gradlew.bat` instead of `./gradlew`. Enable USB debugging and accept the phone's prompt. If multiple devices are connected, use `adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk`.

APK: `app/build/outputs/apk/debug/app-debug.apk`. VS Code tasks are under Terminal → Run Task. No F5 Android debugger is configured; use adb logcat or Android Studio's debugger.

### Gradle and font assets

The provided gradlew/gradlew.bat run tools/gradle.py and are **not the official Gradle Wrapper**. They download Gradle 8.11.1, validate against the upstream HTTPS SHA-256 checksum and cache it under Gradle user home. Run one first build at a time. With Gradle 8.11.1 installed, `gradle :app:assembleDebug` works without Python. To create a standard wrapper, run `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`, pin its distribution checksum and commit the generated wrapper files. Never commit signing keys or local.properties.

prepareJapaneseFonts downloads four static font files and two OFL notices from an immutable google/fonts revision with hash validation. It packages them under app/build/generated/japaneseAssets; no runtime font download is needed. Fonts add about 15.6 MB before compression. A clean first build needs access to raw.githubusercontent.com. Build failure is deliberate if fonts cannot be verified.

## First use

1. Studio: choose a photo or gradient. Photos are limited to 40 MB; JPEG/PNG are safest. Animated inputs are used as static images.
2. Words: use the 50 starter entries, or select JLPT levels and tap Download / refresh selected levels. Disable Include starter pack for strictly level-filtered study. The provider is a third-party study resource, not an official JLPT syllabus.
3. Select a word and configure Line designer. Layout templates support {word}, {reading}, {meaning}. One slot is one visual line; long content shrinks, then ellipsizes if necessary.
4. Save line layout or Save & apply. Position/global-size/panel sliders save on release; per-line drafts must be saved.
5. Optional Schedule: explicitly enable screen-off updates (notification required), or select a timed interval. To stop, use the app or the ongoing notification.

The preview clock is simulated and never burned into wallpaper. Android controls the real clock/notifications/fingerprint indicators. Vocabulary is not an interactive overlay. WallpaperManager.FLAG_LOCK is the only requested destination; OEM cropping/integration can vary.

## Architecture and tests

- WallRepository: shared settings, cached dictionary, photo IO, preview/export/apply, serialized by a coroutine mutex. Network fetches occur outside the wallpaper mutex.
- WallpaperRenderer/JapaneseFonts: sampled EXIF-aware photo rendering, StaticLayout and shared asset-backed Japanese fonts for preview/export/wallpaper.
- Typography/Codec/Editor: persisted line customization and live drafts.
- JlptClient/VocabularyCache/WordPolicy: validated API parsing, private atomic per-level cache, level/favorite-aware sequential or shuffled selection.
- MainActivity/StudioScreen/WordLibrary/WallViewModel: Compose UI and state.
- ScreenCycleService/RotationWorker: opt-in background update modes.
- assets/words.json: editable starter list; keep unique IDs and correct readings.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
# With a connected device/emulator:
./gradlew :app:connectedDebugAndroidTest
```

Unit tests cover math, templates, filters and API parsing. Device tests check rendering, preview/export equality and different Japanese-font output. They do not replace actual visual/wallpaper testing. See docs/V1_3.md and docs/DEVICE_TEST_CHECKLIST.md.

## Limits and troubleshooting

- No in-app dictionary editor, arbitrary font-file import, always-on-display integration, clickable wallpaper, home-screen target or live wallpaper.
- Existing system wallpaper and exact clock layout cannot be automatically restored/read on modern Android. Clearing app data removes photo/settings/cache, not the wallpaper already applied.
- Output follows physical portrait display proportions, capped within 1440×3200. Foldables/landscape/OEM cropping need device tests.
- Downloads depend on provider uptime and data quality. Errors retain prior cached data; empty eligible pools do not silently use another level. Search does not change rotation filters.
- Automatic mode replaces wallpapers set elsewhere. Disable it to preserve external changes. An in-flight write can complete around the time Stop is pressed. Reopen/resume after force-stop; OEM battery restrictions may interfere.
- No release signing is configured. GitHub debug signing keys may differ between hosted runs. Uninstalling to fix a signature mismatch deletes saved photo/settings/cache; preserve anything needed first.
- SDK location error: set ANDROID_HOME or local.properties. Java error: use JDK 17. Downloads: check services.gradle.org, Maven repositories and raw.githubusercontent.com. Missing adb: add platform-tools to PATH. Photo failure: use smaller JPEG/PNG. Wallpaper blocked: check device/work-profile policy. Crash logs: `adb logcat -s AndroidRuntime`.

Before public distribution, review current Play requirements, foreground-service declarations, privacy disclosures and upstream data/font licenses. App source is MIT; see LICENSE and THIRD_PARTY_NOTICES.md.
