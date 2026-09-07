# Kumo · 雲

A lightweight Japanese-learning lock-screen app for Android, built with Kotlin, Jetpack Compose and Material 3. Kumo (くも / 雲) means cloud, and the app icon is that kanji. Formerly Kotoba Wall.

Kumo renders a vocabulary card on your own background and sets it as your lock-screen wallpaper, on demand or automatically. Everything is drawn on the device.

## Features

### Words

- 50 offline starter entries, usable immediately without an account or a download.
- Optional JLPT N5–N1 vocabulary downloads, cached privately for offline study.
- Favourites and rotation filters decide which words can reach your lock screen.
- Romaji comes from the vocabulary service when it supplies one, and is otherwise written on the device from the kana reading in modified Hepburn, with macrons for long vowels.

### Studio

- Live preview of the card with position control and two- or three-line layouts.
- Bundled Japanese typography: Zen Kaku Gothic New and Zen Old Mincho in real regular and bold weights, verified by hash at build time.
- PNG export of the current card. The preview clock is only a guide and is never baked into the wallpaper.

### Wallpapers

- **Discover**: Pexels as the default source, with Featured, topic search and shape filters, or Unsplash via Lorem Picsum with no key at all. The Pexels key is entered on the device and encrypted with Android Keystore, never bundled in the APK.
- **Saved**: an offline collection with Last used, plus static or rotating background choices.
- **My background**: choose or import your own photo, apply gradients and crop it. Modern Android restricts reading the system wallpaper, so pick the original image when you want it back.

### Schedule

- Opt-in screen-off updates through a foreground service with an ongoing notification and an explicit Stop control.
- Timed updates as an alternative trigger: every 6 hours, every 12 hours or daily.
- Clear status for the active mode, the last applied wallpaper and any error Android reported.

### Appearance

- Light, dark and system theme switcher in the top bar, directly before the info button, remembered on the device.
- Material You dynamic colour on Android 12+, with a hand-tuned Kumo blue palette on earlier releases.
- Edge-to-edge layout; status and navigation bar icons follow the appearance you choose.

### Icon and identity

- The kanji 雲 in white on Kumo blue #235BB5: vector fallback for API 24–25, adaptive icon for API 26+, monochrome themed icon for API 33+, and a matching notification silhouette.
- A compact set of original interface glyphs instead of `material-icons-extended`, which keeps the dependency and APK footprint small.

### Privacy

- No account, no analytics, no uploads. Cards are rendered on your device from your own vocabulary and photos.
- Photo providers receive your IP address and search terms; the vocabulary provider receives your IP address and requested level. Saved backgrounds, Last used and API keys stay in private app storage.

## Build

JDK 17, Gradle 8.11.1, Android SDK 35 with build-tools 35.0.0, minSdk 24. The first build needs internet access for dependencies and the four hash-verified Japanese fonts.

```sh
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Debug output: `app/build/outputs/apk/debug/app-debug.apk`. The bundled `gradlew` scripts are Python convenience launchers, not the official Gradle Wrapper. An editor such as VS Code does not include the Android SDK; install it separately.

## Production build

Follow [docs/PRODUCTION_RELEASE.md](docs/PRODUCTION_RELEASE.md): create or reuse a stable private signing key, add the four Actions secrets, then run **Actions → Kumo production release → Run workflow**. The run produces a signed APK and AAB, the R8 mapping file, SHA-256 checksums and a measured size report as artifacts. Nothing is uploaded to Google Play.

Release packaging fails when signing is not configured; there is no fallback to a debug signature. Never commit signing material or API keys. A production key normally differs from a debug key, so Android may refuse an in-place update, and uninstalling deletes saved photos, vocabulary and settings. Same-key updates keep your data because the application ID stays `com.kotobawall.app`.

## Releases

Every version is published on GitHub automatically. Bump `versionCode` and `versionName` in `app/build.gradle.kts` on `main`, and the **Kumo version release** workflow creates the matching `v<versionName>` tag and release. Notes come from `docs/releases/v<versionName>.md` when that file exists, and from generated notes otherwise. Existing tags and releases are never overwritten. Attach the signed binaries from the production workflow when you want them distributed.

## Use

1. Open **Words**, keep the starter list or download a JLPT level, and mark favourites.
2. Open **Wallpapers**, pick a background from Discover, your saved collection or your own photo.
3. Open **Studio**, adjust font, layout and position, then apply the card to your lock screen or export a PNG.
4. Open **Schedule** if you want updates to happen on their own, and pick screen-off or timed updates.

## Validation and limitations

- The production workflow runs release unit tests and lint, verifies the APK signature and reports actual debug and release sizes. It is not device QA and not a store review.
- Source review and icon rendering checks are not an Android build. Test on real phones, including release/R8 mode, fonts, photo import, key storage and foreground-service behaviour, before distributing.
- Background updates depend on Android. Battery saving, OEM restrictions and force-stopping the app can delay or stop them, and timed intervals are approximate.
- The bundled Japanese fonts keep all of their original glyphs; nothing was stripped to reach a size target.

## More

[Brand assets](branding/README.md) · [device checklist](docs/DEVICE_TEST_CHECKLIST.md) · [third-party notices](THIRD_PARTY_NOTICES.md)

Review provider licences and current store, foreground-service and privacy requirements before publishing. App source is MIT-licensed; bundled fonts and provider data are not.
