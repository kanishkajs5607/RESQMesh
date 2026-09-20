# Three-phone acceptance test

Status: the project team confirms a successful three-phone offline A → B → C demonstration with a 2-hop path. The steps below are a repeatable demo script; restart, reconnection and other additional checks are not claimed as already performed.

## Before the demo

1. Download [the existing v1.0.0 APK](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0) and install the SAME APK on three Android 8+ phones with Google Play services.
2. Label them A (victim), B (relay), C (rescuer). Note each eight-character node ID.
3. Open RESQMesh once on all phones and allow requested Nearby permissions.
4. Disable mobile data. For the stricter test, enable airplane mode, then manually turn Bluetooth and Wi-Fi back ON. No router/internet connection is required.
5. Enable Android Location services if discovery fails, especially on Android 8–12L.
6. Keep RESQMesh visible. It intentionally pauses networking when hidden. Stored messages survive app closure.
7. Do not create a new SOS on every retry: the SAME packet ID must travel A → B → C.

## Prove a real A → B → C relay

1. Leave C's mesh PAUSED. Select Citizen Mode on A and B.
2. Tap Start mesh on A and B. Tap Connect on ONE phone's node card.
3. Compare the connection code and tap Codes match on BOTH phones.
4. A: Create SOS → manually enter Collapse, CRITICAL, 4 people, 2 injured and “Trapped under building”. Use available last-known coordinates, explicitly labelled manual/demo coordinates, or no location. The built-in example uses different counts; edit them for this script.
5. A: Save & Share SOS.
6. B: confirm the emergency card appears, hop count is 1, path is A → B, and its message ID matches A's. A's activity must say Stored by B.
7. PAUSE A's mesh. Keep A paused throughout the rest of the demo. This prevents an A → C shortcut, regardless of physical distance.
8. Optional persistence check: on B, pause mesh, fully close the app, reopen it and verify the SAME packet remains saved. This is an additional check, not a reported outcome of the earlier three-phone demonstration.
9. C: select Rescuer Mode and Start mesh. B: Start mesh; connect B and C, verifying codes on both.
10. The saved SOS forwards automatically after connection. C must show:
    - CRITICAL SOS / 4 people / 2 injured;
    - Trapped under building;
    - the original coordinates and timestamp;
    - the SAME message ID and origin A;
    - hop count 2 and path A → B → C.
11. Optional duplicate check: disconnect and reconnect B and C. The inbox must still contain one copy of this packet.
12. B's relayed count should become 1 after C confirms durable storage.

## Exact narration

“All three phones have mobile data off. C is paused, so A can only send to B.
B has now stored the emergency. We pause A so it cannot send directly to C.
Now B meets C. C receives the original SOS with two hops and the path A to B to C.
The emergency moved through local radios, without a cloud server.”

## Additional checks — not yet reported as performed

| Test | Expected |
|---|---|
| Reject pairing code | No connection or SOS sharing |
| Location denied/empty | SOS still saves without coordinates |
| Invalid coordinates/counts | Form explains error and does not submit |
| Pause all nodes, create SOS, reconnect | Saved SOS transfers later |
| Receive NORMAL then CRITICAL | CRITICAL displayed first |
| Close/reopen after receipt | Same ID, origin, coordinates and hop count |
| Disconnect during transfer | Packet stays saved; retry/reconnect possible |
| Reconnect after successful receipt | No duplicate card |
| Stop after maximum hop | Packet displayed locally but not forwarded |

If discovery stalls: verify Bluetooth/Wi-Fi, Nearby permissions and Location services, pause/start on both phones, and connect from only one side. Each connection needs both confirmations. After three unacknowledged sends in a connection, reconnect to retry. Do not claim rescue delivery based only on local save.
