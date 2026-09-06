# Kumo · 雲

A lightweight Japanese-learning lock-screen app, formerly Kotoba Wall. Kotlin, Jetpack Compose and Material 3. Kumo (くも / 雲) means cloud.

## Version 1.6

- White cloud on the original #235BB5 blue. SVG design sources, adaptive Android icons, monochrome themed icons and notification silhouette.
- Compact original UI vector set replaces material-icons-extended to reduce dependency overhead. Japanese fonts and real regular/bold weights remain intact.
- Production signing configuration, release R8/resource shrinking, APK/AAB outputs and a manual workflow template with actual size reporting.
- Existing vocabulary, Pexels/Picsum backgrounds, typography, Last used and screen-off rotation features are retained.

## Build

Use JDK 17, Gradle 8.11.1, Android SDK 35 and build-tools 35.0.0. First builds require internet for dependencies and the four hash-verified Japanese fonts. minSdk is 24.

```sh
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The included gradlew scripts are Python convenience launchers, not the official Gradle Wrapper. VS Code is an editor; install the Android SDK separately. APK output: app/build/outputs/apk/debug/app-debug.apk.

## Production build — one-time setup

Read [PRODUCTION_RELEASE.md](docs/PRODUCTION_RELEASE.md). Add the prepared [workflow](docs/workflows/kumo-release.yml) as `.github/workflows/kumo-release.yml` on main using your own GitHub access, then configure the four signing secrets. The documentation copy does not run by itself. Trigger **Actions → Kumo production release → Run workflow**. No automatic Play upload or public GitHub release is performed.

Production packaging requires a stable private signing key and fails without it. Never commit signing material or API credentials. A production key normally differs from the current debug key: Android may reject an in-place update. Do not uninstall before preserving your original photos and anything you need; uninstalling clears app data. Same-key updates retain data because the application ID remains com.kotobawall.app.

## Use

- **Wallpapers → Discover:** Pexels (default, on-device encrypted API key), or Unsplash via Picsum (no key, browse-only). Pexels has Featured, topic search and shape filters.
- **Wallpapers → Saved:** offline collection, clean Last used and static/rotating background options.
- **Wallpapers → My background:** choose/import a photo, gradients and crop. Modern Android restricts system wallpaper access; choose the original image when needed.
- **Words:** starter vocabulary, optional JLPT N5–N1 downloads, favorites and rotation filters.
- **Studio:** live preview, position, two/three-line text layout, Japanese fonts and PNG export. The clock is only a guide and is not baked into wallpaper.
- **Schedule:** opt-in screen-off service with ongoing notification, or approximate timed word changes. OEM battery limits can delay/stop updates.

## Validation and size

No measured APK reduction is claimed before a build completes. The production workflow reports actual debug/release sizes and verifies the APK signature; device QA is still required. The four bundled Japanese fonts retain all their original glyphs. Source and icon-rendering checks are not an Android build or device test.

See [brand assets](branding/README.md), [v1.5 features](docs/V1_5.md), [v1.4 behavior](docs/V1_4.md), [device checklist](docs/DEVICE_TEST_CHECKLIST.md), and [third-party notices](THIRD_PARTY_NOTICES.md). Review provider licenses and current store/foreground-service/privacy requirements before distributing. Source is MIT.
