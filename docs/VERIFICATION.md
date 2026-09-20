# Verification record

## Automated results

- Android debug compilation and APK packaging: PASS.
- Core protocol JUnit tests: 13 passed, 0 failures/errors/skips.
- SQLite Robolectric tests on Android API 28: 2 passed, 0 failures/errors/skips.
- Android lint: PASS, zero errors, three warnings.
- Android XML/workflow YAML syntax and Unix wrapper shell syntax: PASS.
- Gradle wrapper JAR archive integrity: PASS.

Lint warnings: two permission flags are ignored on Android versions earlier than API 31 (expected); explicit Android 12+ backup/data-extraction rules are a production follow-up. No warnings were suppressed to obtain the result.

## Build environment and recovery

Built using Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3 and compile SDK 35. Tooling was installed during this task. Initial dependency downloads failed until the workspace JVM proxy was configured. Robolectric's separate downloader also needed proxy forwarding. An API-28 test cleanup helper was corrected to close SQLiteOpenHelper explicitly instead of relying on newer AutoCloseable behavior.

Final verification command:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The local run used the same Gradle 8.9 distribution directly plus environment-specific proxy flags and a two-worker limit. Proxy addresses and local SDK paths are not committed or packaged.

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions has NOT run. Automatic approval review rejected remote repository writes because of the earlier inspect-only instruction. No commits or pushes have occurred. Explicit chat approval is still needed to publish this project to the existing repository.

## Passing test coverage

- Full packet JSON round-trip (including Tamil text).
- A → B → C routing retains origin, coordinates, creation time and message ID.
- Hop count and TTL decrement through eight hops.
- Path loop prevention; wrong immediate sender rejection.
- Malformed path, inconsistent TTL, invalid coordinates, oversized payload and invalid injured count rejection.
- Missing location allowed.
- Priority ordering and newest-first ordering within priority.
- SQLite duplicate suppression and receipt idempotence.
- Packet and acknowledgement persistence after database close/reopen.
- Persisted relay path and relay counter behavior.

## Requires phones

Installability; Google Play services/radio compatibility; Nearby permissions by Android version; actual A → B transfer; B → C transfer after A is paused; packet persistence after app restart; pair-code rejection; real UI layout on small screens; airplane-mode operation with radios manually re-enabled.

See THREE_PHONE_DEMO.md for pass/fail observations to record.
