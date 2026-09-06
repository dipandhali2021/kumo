# Kumo production release

## One-time workflow installation

The connected GitHub integration previously denied writes under `.github/workflows`. The prepared workflow is stored as documentation at `docs/workflows/kumo-release.yml`; this file does not run there.

In GitHub, create `.github/workflows/kumo-release.yml` on `main`, copy in the complete contents of `docs/workflows/kumo-release.yml`, and commit. Alternatively add the downloaded workflow file with that exact path using your own Git checkout. Do not replace the existing debug workflow. The production workflow has only `workflow_dispatch`: no automatic production build on push and no automatic publication to GitHub Releases or Google Play.

## Create or reuse a stable private signing key

If this app already has a production signing key, reuse it. Otherwise generate one locally, outside the repository:

```sh
mkdir -p "$HOME/kumo-signing"
chmod 700 "$HOME/kumo-signing"
keytool -genkeypair -v -keystore "$HOME/kumo-signing/kumo-release.jks" \
  -storetype JKS -alias kumo -keyalg RSA -keysize 3072 -validity 10000
```

The command prompts for passwords and certificate details; do not put passwords into shell commands, source code, or chat. Keep encrypted offline backups of the keystore, alias and passwords. Do not generate a new key on every build. Losing the signing key may prevent future updates.

Base64-encode the keystore locally (encoding is NOT encryption):

```sh
python3 -c 'import base64,pathlib; p=pathlib.Path.home()/"kumo-signing"; (p/"keystore-base64.txt").write_text(base64.b64encode((p/"kumo-release.jks").read_bytes()).decode())'
chmod 600 "$HOME/kumo-signing/keystore-base64.txt"
```

In the repository, open **Settings → Secrets and variables → Actions → New repository secret**, and add:

| Secret | Value |
|---|---|
| `KUMO_KEYSTORE_BASE64` | Entire content of the local keystore-base64.txt file |
| `KUMO_KEYSTORE_PASSWORD` | Keystore password |
| `KUMO_KEY_ALIAS` | `kumo`, or your existing signing alias |
| `KUMO_KEY_PASSWORD` | Key password; often the same as the keystore password |

Never commit these values or the keystore. The workflow decodes the key to a temporary runner file, uses secrets only in signing steps, then removes the temporary file even after failure. It never embeds the Pexels API key. Only run this workflow on trusted code: build scripts can read signing environment variables. For stronger access control, add a GitHub deployment environment with required reviewers and environment secrets before distribution.

## Trigger a build

1. Open **Actions → Kumo production release → Run workflow**.
2. Select the trusted `main` branch and run it.
3. After all steps succeed, download **Kumo-production-RUN_NUMBER** from that run's Artifacts section.

The ZIP contains a signed `Kumo-release.apk`, signed `Kumo-release.aab`, R8 `mapping.txt`, SHA-256 checksums, and an actual APK size report. APK is for installation; AAB is for store upload, not direct installation. Artifacts are retained for 14 days. Save the signing key and mapping file for every distributed version. Increment versionCode before distributing an update; the current version is 1.6.0 / code 7.

The workflow runs release JVM tests and release lint, builds release APK/AAB plus a debug APK for size comparison, and verifies the APK signature. It does not run an emulator, device tests, or a Play policy review. Do not equate a green workflow with device QA or store approval.

## Local signed build

Set `KUMO_KEYSTORE_PATH`, `KUMO_KEYSTORE_PASSWORD`, `KUMO_KEY_ALIAS`, and `KUMO_KEY_PASSWORD` in a secure local environment, then run:

```sh
gradle :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Release packaging fails if signing configuration is missing; there is no fallback to a debug signature. Debug builds still work without signing secrets. Toolchain: JDK 17, Gradle 8.11.1, Android SDK 35/build-tools 35.0.0.

## APK size changes

- Removed `material-icons-extended`. The UI uses 19 small original vector glyphs; Material 3 components are retained. This targets debug APK overhead as well as keeping release lean.
- Production uses the existing R8 code optimization/obfuscation and resource shrinking, with debug tooling excluded. Added narrow keep rules for the ViewModel constructors used reflectively.
- All four real Japanese regular/bold font files remain intact. No kana, kanji, user text, JLPT levels or font choices were removed to reach an arbitrary size target.
- Launcher and notification icons are vectors, not multiple large raster images.
- AAB output enables device-specific store delivery; its file size is not the installed APK size.

The earlier 27 MB debug APK was not provided for analysis. No final size or percentage reduction has been measured here. The workflow publishes a measured same-commit debug/release breakdown, including compressed fonts and DEX.

## Upgrade and distribution warning

The application ID remains `com.kotobawall.app` so same-key updates retain existing settings, photos, vocabulary and the on-device Pexels credential. Kumo is a display-name change, not a separate app identity.

A new production key differs from your existing debug key. Android normally refuses to install one over the other. Do NOT uninstall blindly: uninstalling deletes the saved collection, settings, vocabulary cache and API key. Preserve original photos and anything else you need before a deliberate migration. Future production updates must use the same signing key.

Before public distribution, test on real phones (including release/R8 mode, fonts, wallpaper import, Pexels key storage and foreground-service behavior), review provider licenses/privacy/foreground-service declarations, and check current Play target-SDK and publication requirements.
