# FocusGuard 2.0

FocusGuard is an Android screen-time tracker with user-selected Focus Breakers, five-minute snooze, daily summaries and reminders, and a Flutter Insights screen. Tracking starts with explicit consent and can be stopped from the dashboard or persistent notification. Usage data stays on the device. See the [privacy policy](PRIVACY_POLICY.md).

## Build and verify

Use JDK 21, Android SDK 36, and Flutter 3.41.7.

```sh
(cd flutter_nudge && flutter pub get --enforce-lockfile && flutter analyze && flutter test)
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

Use `gradlew.bat` on Windows. To sign a release, supply the existing release keystore through `FG_KEYSTORE`, `FG_STORE_PASSWORD`, `FG_KEY_ALIAS`, and `FG_KEY_PASSWORD`. Without these variables, the release package is for verification only. Keep signing material outside the repository.

## Release verification

- Run the Android release workflow and install the minified signed candidate.
- Verify usage-access denial and revocation, overlay refusal, notification refusal, reboot, manual stop, and battery-restricted operation on Android 8 and Android 15/16, including a Samsung device.
- Check app selection, real usage totals, block counts, five-minute Snooze, strictness, daily goal, reminders, summary, Insights, and data clearing.
- Confirm the package ID, version code, signing identity, and 16 KB alignment. Complete Play Console Data safety and declare the user-visible `specialUse` foreground service for review.

The historical TFLite asset lacks matching preprocessing metadata. The app uses duration-based nudges until the model asset has matching, checksum-bound metadata. The generator exports matching model and preprocessing files. Its data is synthetic. Evaluate predictive accuracy on held-out real-world data before deploying a learned model.
