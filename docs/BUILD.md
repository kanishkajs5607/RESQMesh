# Build and test

For installation without development tools, download [RESQMesh-debug.apk from v1.0.0](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0).

## Toolchain

JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The repository includes the standard Gradle wrapper pinned to 8.9 with a distribution SHA-256 checksum. Android Gradle Plugin is 8.7.3; Kotlin is 1.9.24.

Open the project root in Android Studio. Set `ANDROID_HOME` to the Android SDK or use an untracked `local.properties`, for example:

```properties
sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
```

First-time dependency/tool downloads need internet. The installed app's local relay does not.

### Windows

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

### macOS / Linux

```sh
chmod +x gradlew
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### Optional USB installation

```sh
adb devices
adb -s PHONE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Install the same APK on all participating phones. Separate debug builds may use different signing keys; Android can refuse an update signed by a different key. Uninstalling clears saved SOS data, so do not do this during persistence tests.

## GitHub Actions

The [Android build workflow](../.github/workflows/android.yml) runs tests, lint and APK packaging on pushes to main, pull requests or manual dispatch. A successful run uploads `RESQMesh-debug` and `verification-reports` artifacts. Use the published v1.0.0 release for the physically tested submission APK.

The initial CI run failed in SDK setup because the action requested the unavailable legacy `tools` package, before compilation. The workflow now explicitly requests `platform-tools`; SDK Platform 35 and Build Tools are installed in the existing following step. Check [Actions](https://github.com/kanishkajs5607/RESQMesh/actions) for the current result rather than assuming a passing build.

The GitHub release tag is `v1.0.0`; the APK's internal Android version remains `0.1.0` / code `1`. This documentation polish does not rebuild or replace the tested release.

See [verification](VERIFICATION.md) and the [physical demo](THREE_PHONE_DEMO.md).
