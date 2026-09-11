# Local APK Signer

An offline, serverless Android app that signs APKs directly on the device using CodeAssist's apksigner integration.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Run the app on an emulator or physical device

## Build Unsigned Release APK via GitHub Actions

Every push (to `main`/`master`) and every pull request automatically triggers a build in GitHub Actions (see `.github/workflows/build.yml`). The workflow builds **one unsigned release APK** (`app-release-unsigned.apk`) and uploads it as a downloadable artifact — no keystore, no secrets, no signing.

After a successful run: go to your repo → **Actions** tab → click the latest run → scroll down to **Artifacts** → download `unsigned-release-apk`.

You can also trigger the build manually from the Actions tab (**Run workflow** button).

## Signing the APK yourself (after download)

The artifact is unsigned, so Android will refuse to install it as-is. Sign it locally with your own keystore using Android SDK's `apksigner`:

```bash
# Koyekta jinish lagbe apner device e: Android SDK er build-tools
apksigner sign --ks my-upload-key.jks --out LocalApkSigner-signed.apk app-release-unsigned.apk

# Verify:
apksigner verify --print-certs LocalApkSigner-signed.apk
```

Noye keystore banate:

```bash
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

(Note: ei app ta nije-i ekta on-device APK signer — tamey ei app diye o unsigned APK sign korte paro.)

## Project stack

- Kotlin 2.2 / Jetpack Compose
- AGP 9.1.1, Gradle 9.3.1
- compileSdk 36, minSdk 24
- Room, Retrofit/OkHttp/Moshi, Firebase AI (Gemini)
- Roborazzi + Robolectric for screenshot/unit tests
