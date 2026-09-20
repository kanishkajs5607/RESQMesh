# RESQMesh
### Communication when networks fail.

RESQMesh is an infrastructure-free Android emergency communication prototype. Nearby phones running the app exchange SOS messages without internet or mobile data, storing and forwarding them until they reach a rescuer.

> **Working three-phone prototype:** physically demonstrated **Citizen A → Relay B → Rescuer C**, with the original SOS arriving over a **2-hop relay path**.

**[Download RESQMesh-debug.apk](https://github.com/kanishkajs5607/RESQMesh/releases/download/v1.0.0/RESQMesh-debug.apk)** · [v1.0.0 release](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0) · [Three-phone demo guide](docs/THREE_PHONE_DEMO.md)

## The Problem

Floods, earthquakes and building collapses can disrupt cellular towers, mobile data and internet connectivity. Victims may still have working phones but lose the ability to communicate their location, injuries and rescue needs.

## Our Solution

RESQMesh uses local phone-to-phone communication to carry emergency information through participating devices. Citizen Mode creates SOS messages; Rescuer Mode makes received emergencies easy to review. Both modes can relay messages.

**All participating phones must have RESQMesh installed. No router, cloud backend or mobile-data connection is needed for the relay.**

## How RESQMesh Works

**Victim phone → Nearby relay phone → Additional relay(s), if needed → Rescuer phone**

1. **STORE:** Save the SOS on the sender and each receiving phone.
2. **CARRY:** Retain the message while a phone moves or has no connected peer.
3. **FORWARD:** When another eligible phone connects, forward the saved SOS.

Users discover nearby nodes, connect and compare the pairing code on both phones. Eligible saved messages then forward automatically. Keep the app visible while connecting and relaying.

## Key Features

- Offline device-to-device communication and nearby-node discovery.
- Store-and-forward SOS relaying and multi-device propagation.
- Citizen and Rescuer modes with emergency priority handling.
- Persistent SOS storage and duplicate-message prevention.
- Hop-count tracking and relay-path visualization.
- Last-known location sharing, with manual or unavailable-location options.

## SOS Packet

| Information | Included details |
|---|---|
| Emergency | Type, description and priority |
| People | Number of people and number injured |
| Location | Last-known coordinates, source and fix time when available |
| Origin | Unique message ID, anonymous origin device ID and creation timestamp |
| Journey | Hop count, remaining-hop TTL and relay path |

Original emergency information is retained while hop count and path are updated at each receiver.

## Three-Phone Physical Demo

> **Successfully tested on THREE physical Android phones without internet/mobile data.**

| Phone | Role | Demonstrated result |
|---|---|---|
| **A** | Citizen / victim | Created an SOS |
| **B** | Relay | Received, stored and relayed the SOS |
| **C** | Rescuer | Received the original SOS in Rescuer Mode, showing **2 hops** |

This result is reported by the project team and recorded in the [published release](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0). It establishes the three-phone demonstration, not a claim of certified reliability or larger-scale field testing. [Verification details](docs/VERIFICATION.md).

## Screenshots / Prototype Proof

Real screenshots are awaiting upload; no placeholder or generated images are presented as evidence.

Upload them to **`docs/screenshots/`** using the filenames and display instructions in the [screenshot guide](docs/screenshots/README.md). Prioritize the Rescuer card showing the CRITICAL SOS and **2-hop path**, followed by the physical-device photo.

## Technology / Architecture

| Layer | Actual implementation |
|---|---|
| Android app | Kotlin, Jetpack Compose and Material 3; Android 8.0+ |
| Discovery / transport | Google Nearby Connections, `P2P_CLUSTER`, using local Bluetooth/BLE/Wi-Fi capabilities |
| Persistent storage | SQLite through `SQLiteOpenHelper`; packets and per-peer receipts |
| Protocol / routing | Kotlin core module; `kotlinx.serialization` JSON, validation and forwarding rules |
| Coordination | `AndroidViewModel`, coroutines and `StateFlow` |
| Location / identity | Android `LocationManager` cached fixes; anonymous UUID saved in `SharedPreferences` |
| Verification | JUnit protocol tests and Robolectric SQLite tests |

`app/` contains the UI, transport and storage; `core/` contains packet/routing logic. This is application-level relaying over local links, not a replacement for internet access. Google Play services is required.

## Reliability / Routing

- **Unique UUIDs + SQLite primary keys** prevent duplicate inbox entries.
- **Hop count starts at 0; TTL starts at 8.** Each hop increments the count and reduces TTL. TTL means remaining hops, not message age.
- **Loop protection** excludes nodes already in the path and peers that acknowledged storage.
- **Durable receipts:** acknowledgement is sent after the receiver saves the packet.
- **Priority:** CRITICAL → HIGH → NORMAL; newest first within each priority.
- **Bounded retries:** up to three unacknowledged attempts per packet per connection; reconnect to retry after exhaustion.

A storage confirmation means a peer saved the SOS; it does not mean a rescue team has acted.

## Installation

**[Get the existing v1.0.0 release](https://github.com/kanishkajs5607/RESQMesh/releases/tag/v1.0.0) — RESQMesh v1.0.0 – HackDay 1.0 Prototype**

1. Download **RESQMesh-debug.apk** from the release.
2. Install the same APK on each Android phone; allow installation from that source if prompted.
3. Grant the requested **Nearby Devices / Location** permissions.
4. Enable **Bluetooth and Wi-Fi**. Use Android 8.0+ phones with Google Play services.
5. **Internet/mobile data is not required** for mesh communication.
6. Open RESQMesh and tap **Start mesh** on participating phones. Tap **Connect**, compare codes and confirm on both devices.

Keep the app open while relaying. For airplane-mode testing, manually re-enable Bluetooth/Wi-Fi afterward; this is a test instruction, not a claim that airplane mode was part of the reported demo.

Developers: [build and test instructions](docs/BUILD.md).

## Demo Scenario

1. **A — Citizen:** manually create **CRITICAL / Collapse**, **4 people**, **2 injured**, message **“Trapped under building”**. Use available last-known coordinates or clearly label a manual/demo location.
2. **B — Relay:** connect A and B; B receives and stores the SOS. Keep C paused initially.
3. **C — Rescuer:** pause A, then connect B to C. B forwards the saved packet; C displays the original information and **A → B → C**, hop count **2**.

This is the repeatable submission script; these exact message/count values are not asserted as the contents of the earlier physical test. [Detailed steps](docs/THREE_PHONE_DEMO.md).

## Privacy

No registration, phone number or contacts are required. The prototype uses anonymous device identifiers and rescue-relevant information. Connected participants can read the shared SOS and coordinates. Nearby links are encrypted and pairing codes are checked, but packets are not end-to-end encrypted or cryptographically signed; Rescuer Mode does not verify a person's identity.

## Limitations

- Hackathon prototype, **not a certified emergency service** or guaranteed rescue channel.
- Every relay phone needs RESQMesh, compatible radios, permissions and Google Play services.
- Networking pauses when the app is hidden; background relaying is not implemented.
- Range and discovery depend on hardware and surroundings; larger-scale operation is unverified.
- Cached coordinates can be old or unavailable. No live-map or fresh-GPS guarantee.
- Packets persist locally; hop TTL does not expire them by age. Local storage is not separately encrypted.

## Future Scope

Background relaying, larger-scale mesh testing, stronger authentication and encryption, battery optimization, integration with official disaster-response systems, and optional gateway synchronization when a node regains internet connectivity.

## HackDay 1.0

Developed as a prototype for **HackDay 1.0** under the theme **“TECH FOR A BETTER TOMORROW”**.
