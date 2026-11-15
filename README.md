# FocusGuard 2.0

A native Android digital-wellbeing application for monitoring app usage and delivering focus nudges.

## Implemented components

- Java Android application in `app/`, targeting Android API 34 with minimum API 26.
- Room persistence for tracked apps, usage and daily statistics.
- App selection, onboarding, settings and usage-oriented screens.
- Bundled TensorFlow Lite asset and a separate model-generation script.
- Experimental Flutter nudge module in `flutter_nudge/`.

## Build from source

Open the repository in Android Studio, install Android SDK 34 and use the JDK required by the checked-in Gradle/Android plugin versions. Let Gradle sync before building. From a configured terminal:

```sh
cd flutter_nudge
flutter pub get
cd ..
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

On Windows use `gradlew.bat` instead. Debug artifacts appear under `app/build/outputs/apk/`; committed historical build artifacts are not a substitute for a fresh build.

## Verification and limits

Test permission denial, usage access, background execution, notification behavior and battery restrictions on an Android 8+ device. The TFLite asset needs a documented dataset, held-out evaluation and model-version tracking before making predictive-accuracy claims. The Flutter module is a separate prototype, not proof of full platform integration.

## Project hygiene

Keep `local.properties`, signing keys, personal usage logs and new build outputs untracked. Existing generated files in Git history are retained; `.gitignore` alone cannot remove already tracked files. Configure a release application ID and signing process before distribution.

The settings file includes the generated Flutter Android module; install Flutter and run `flutter pub get` in `flutter_nudge` before Gradle sync. The daemon configuration requests JetBrains JDK 21.
