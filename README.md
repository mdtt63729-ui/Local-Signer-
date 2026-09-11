# Local APK Signer

An offline, serverless Android app that signs APKs directly on the device using CodeAssist's apksigner integration.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Build via GitHub Actions

Every push (to `main`/`master`) and every pull request automatically triggers a build in GitHub Actions (see `.github/workflows/build.yml`). The workflow builds both a **debug APK** and a **release APK**, runs unit tests, and uploads the APKs as downloadable artifacts.

After a successful run: go to your repo → **Actions** tab → click the run → scroll down to **Artifacts** to download the APK.

### Signing the release APK (optional but recommended)

The release build is signed with a keystore whose path comes from the `KEYSTORE_PATH` environment variable (defaults to `my-upload-key.jks` in the project root). To sign the release build in CI, set up these GitHub **repository secrets**:

| Secret name | What it is |
|---|---|
| `SIGNING_KEYSTORE` | Your `.jks` keystore file, **base64-encoded** (see below) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key (alias) password — the alias must be `upload` |

To base64-encode your keystore locally:

```bash
base64 -i my-upload-key.jks | tr -d '\n'
```

Copy the entire output, then in your repo go to **Settings → Secrets and variables → Actions → New repository secret**, and paste it as `SIGNING_KEYSTORE`. Add `STORE_PASSWORD` and `KEY_PASSWORD` the same way.

If no secrets are set, the debug APK still builds; only the release APK will fail to sign (it will remain unsigned / the build will error on the release step).

### Keystore alias

The signing config uses the alias **`upload`**. When generating a new keystore, create it with:

```bash
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

## Project stack

- Kotlin 2.2 / Jetpack Compose
- AGP 9.1.1, Gradle 9.3.1
- compileSdk 36, minSdk 24
- Room, Retrofit/OkHttp/Moshi, Firebase AI (Gemini)
- Roborazzi + Robolectric for screenshot/unit tests
