# Verification record

## Three-phone physical demonstration — confirmed by the project team

The team reports a successful offline test on **three physical Android phones**:

- A created an SOS in Citizen Mode.
- B received, stored and relayed the SOS.
- C received the original SOS in Rescuer Mode, displaying a **2-hop relay path**.
- Internet/mobile data was not required for this transfer.

The [v1.0.0 release](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0) also records this demonstration. Its uploaded `RESQMesh-debug.apk` is 10,508,733 bytes.

This is a team-reported hardware result, not a physical test performed by the documentation agent. Exact device models, OS versions, distance, latency, battery life, airplane-mode testing and restart/reconnection test outcomes have not been supplied; no results for those are claimed.

## Recorded automated verification

The original build verification recorded successful debug compilation/APK packaging, **13 passing protocol tests + 2 passing SQLite tests**, and lint with **zero errors and three warnings**. The committed [protocol report](test-results/TEST-org.resqmesh.core.PacketTest.xml) and [storage report](test-results/TEST-org.resqmesh.app.PacketStoreTest.xml) each record zero failures/errors/skips.

Coverage includes JSON serialization, A → B → C routing, immutable origin/location/time, hop/TTL limits, loop prevention, malformed inputs, priority ordering, duplicate suppression and SQLite close/reopen persistence. SQLite tests use Robolectric API 28; they are distinct from physical-phone tests.

Lint warnings concern permission flags ignored before API 31 and explicit Android 12+ backup/data-extraction rules. No application code is changed by this submission polish; the automated suite is not rerun locally solely for documentation edits.

## Repository, release and CI

Source and Gradle configuration are present on main. The existing published v1.0.0 release contains the APK; no replacement release is created.

The [initial Actions run](https://github.com/kanishkajs5607/RESQMesh/actions/runs/35494212101) failed before compilation: SDK setup requested the missing legacy `tools` package. The workflow's package input is corrected to `platform-tools`; see [current Actions runs](https://github.com/kanishkajs5607/RESQMesh/actions) for the new outcome. A failed setup run is not a failed physical demonstration or proof of an application compilation defect.

The submission review checks source-backed technical claims, relative documentation links, release asset metadata, common credential patterns and tracked build-file clutter. This is a basic repository review, not a comprehensive security audit.

## Further tests, not claimed as completed

Broader hardware/OS coverage, measured range/latency, background/screen-off behavior, battery endurance, airplane-mode operation, radio interruption recovery and large networks require their own recorded results. The [demo guide](THREE_PHONE_DEMO.md) includes additional suggested checks; their inclusion is not a claim they were performed.
