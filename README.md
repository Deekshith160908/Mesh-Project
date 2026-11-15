# Off-Grid Multi-Hop Emergency & SOS Mesh Android Application

A production-ready, zero-dependency-on-cellular native Android application built in Kotlin with Jetpack Compose. Designed for disaster relief, search and rescue, wilderness emergencies, and off-grid scenarios where cellular towers, power grids, or internet connections are damaged or non-existent.

---

## 1. High-Level Architecture

The application uses an ad-hoc Peer-to-Peer (P2P) cluster mesh topology powered by the **Google Play Services Nearby Connections API** (`Strategy.P2P_CLUSTER`) running alongside high-accuracy GPS positioning and an offline-first **Room Database**.

```
  +------------------+         P2P BLE / Wi-Fi Direct         +------------------+
  |   Origin Node    | -------------------------------------> |   Relay Node 1   |
  | (Trapped Victim) |                                        | (First Responder)|
  +------------------+                                        +------------------+
           |                                                           |
      Creates SOS                                                 Hop Relayed
      TTL = 5                                                     TTL = 4
      HopPath = "NodeA"                                           HopPath = "NodeA,NodeB"
                                                                       |
                                                              P2P Direct Relay
                                                                       |
                                                                       v
  +------------------+         Opportunistic HTTPS            +------------------+
  | Cloud Emergency  | <===================================== |   Relay Node 2   |
  | Gateway / Server |       (When LTE/Wi-Fi uplink found)    | (Has Cell Reach) |
  +------------------+                                        +------------------+
```

---

## 2. Key Modules & Technical Specifications

### A. Data Layer (`MeshPacket`, `Room`, `DAO`)
* **`MeshPacket`**: Strict domain model containing:
  - `packetId`: UUIDv4 string.
  - `senderId`: Unique device hardware / persistent ID (e.g., `NODE-A7F2`).
  - `packetType`: `"SOS_ALERT"`, `"TEXT_MESSAGE"`, `"PING"`, `"ACK"`.
  - `payload`: Distress description or text message.
  - `latitude` / `longitude`: Exact GPS satellite fix coordinates.
  - `timestamp`: Epoch milliseconds.
  - `ttl`: Time-To-Live integer (starts at 5, decrements on every hop).
  - `hopPath`: Comma-separated list of nodes that handled the packet.
  - `isSyncedToCloud`: Boolean tracking whether the emergency packet has reached the cloud uplink.
* **`MeshPacketDao` & `AppDatabase`**:
  - Persists all received, relayed, and originated packets.
  - Deduplication index prevents redundant writes.
  - Flow queries for real-time Jetpack Compose UI feeds.

### B. Mesh Networking & Multi-Hop Router
* **`NearbyMeshManager`**:
  - Initializes `Strategy.P2P_CLUSTER` which enables simultaneous advertising and discovery.
  - Auto-accepts connections from nearby mesh nodes.
  - Dynamically maintains a registry of active direct peer endpoints.
  - Handles byte-level payload transfer.
* **`MultiHopRouter`**:
  - **Deduplication:** Uses an in-memory LRU set and Room database query. If a `packetId` was already processed, it is immediately dropped.
  - **Loop Prevention:** Inspects `hopPath`. If the local node ID is already in the path, packet is dropped to prevent routing loops.
  - **TTL Expiration:** If `ttl <= 0`, packet is dropped.
  - **Relay Flooding:** If `ttl > 1`, decrements `ttl`, appends local node ID to `hopPath`, and broadcasts to all other connected peers except the sender.
  - **Immediate SOS Alert:** When `packetType == "SOS_ALERT"`, instantly activates the acoustic siren, Morse code vibration, and camera strobe.

### C. Offline GPS Positioning (`LocationProvider`)
* Direct satellite reception via `FusedLocationProviderClient` with `Priority.PRIORITY_HIGH_ACCURACY`.
* Operates fully offline without cellular tower triangulation or A-GPS internet data.
* Coordinates can be copied with one click or opened directly in offline map applications (OsmAnd, Google Maps, etc.) via `geo:` intents.

### D. Emergency Siren, Morse Haptic Vibration & Flashlight Strobe (`EmergencyAlertManager`)
* **SOS Morse Code Vibration:** Haptic motor pulses `... --- ...` pattern (dot = 200ms, dash = 600ms).
* **High-Decibel Siren:** Generates alternating emergency alert frequencies via `ToneGenerator`.
* **Camera Torch Strobe:** Rapidly flashes the device rear LED flash in Morse code SOS for visual search-and-rescue location during nighttime.

### E. Opportunistic Cloud Relay Uplink (`CloudUplinkSyncManager`)
* Automatically monitors network status via `ConnectivityManager.NetworkCallback`.
* When any node in the mesh comes within range of a working cellular tower or satellite Wi-Fi uplink, it queries Room DB for all packets with `isSyncedToCloud == false`.
* Uploads the packets via HTTPS POST to an emergency cloud gateway.
* Marks packets as synced in the local database.

### F. Persistent Background Execution (`MeshForegroundService`)
* Runs as a foreground service with `FOREGROUND_SERVICE_CONNECTED_DEVICE` and `FOREGROUND_SERVICE_LOCATION`.
* Keeps radio discovery and mesh relaying active even when the screen is locked, in pocket, or when running other apps.
* Features a persistent system notification with direct "BROADCAST SOS" action button.

### G. Modern Tactical Jetpack Compose UI
* **SOS Screen (`SosScreen.kt`)**: Giant tactile button with pulsing animation, 3-second countdown cancellation, live GPS status card, distress category selector.
* **Mesh Chat (`MeshChatScreen.kt`)**: Off-grid messaging feed with hop badges (`Direct Hop` vs `2 Hops`), quick canned emergency phrases, and text composer.
* **Radar & Peers (`MeshNodesScreen.kt`)**: Concentric animated radar sweep, active neighbor endpoints, and connection statistics.
* **Packet Log (`PacketLogScreen.kt`)**: Audit trail of every packet traversing the node, cloud sync status, and manual sync triggers.

---

## 3. Project File Tree

```
OffGridMeshSOS/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew.bat
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/emergency/offgridmesh/
│       │   │   ├── MeshApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── alert/
│       │   │   │   └── EmergencyAlertManager.kt
│       │   │   ├── cloud/
│       │   │   │   └── CloudUplinkSyncManager.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── MeshPacketDao.kt
│       │   │   │   │   ├── MeshPacketEntity.kt
│       │   │   │   │   ├── PeerNodeDao.kt
│       │   │   │   │   └── PeerNodeEntity.kt
│       │   │   │   ├── model/
│       │   │   │   │   ├── MeshEvent.kt
│       │   │   │   │   ├── MeshPacket.kt
│       │   │   │   │   └── PeerNode.kt
│       │   │   │   └── repository/
│       │   │   │       └── MeshRepository.kt
│       │   │   ├── location/
│       │   │   │   └── LocationProvider.kt
│       │   │   ├── mesh/
│       │   │   │   ├── MultiHopRouter.kt
│       │   │   │   ├── NearbyMeshManager.kt
│       │   │   │   └── PacketSerializer.kt
│       │   │   ├── service/
│       │   │   │   └── MeshForegroundService.kt
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   │   ├── EmergencyButton.kt
│       │   │   │   │   ├── GpsCoordinatesCard.kt
│       │   │   │   │   ├── MeshStatusBanner.kt
│       │   │   │   │   └── PacketItemView.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── MainScreen.kt
│       │   │   │   │   ├── MeshChatScreen.kt
│       │   │   │   │   ├── MeshNodesScreen.kt
│       │   │   │   │   ├── PacketLogScreen.kt
│       │   │   │   │   └── SosScreen.kt
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   └── viewmodel/
│       │   │   │       ├── MeshViewModel.kt
│       │   │   │       └── MeshViewModelFactory.kt
│       │   │   └── util/
│       │   │       ├── DeviceIdProvider.kt
│       │   │       └── PermissionHelper.kt
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_foreground.xml
│       │       │   └── ic_sos.xml
│       │       ├── mipmap-anydpi-v26/
│       │       │   └── ic_launcher.xml
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/java/com/emergency/offgridmesh/
│           └── MeshPacketTest.kt
```

---

## 4. How to Build & Run

1. Open the `OffGridMeshSOS` project directory in **Android Studio Hedgehog / Iguana / Jellyfish**.
2. Sync Gradle dependencies.
3. Connect 2 or more physical Android devices (Android 8.0+ / API 26+).
4. Run the app on both devices:
   - Accept the requested Nearby Devices and Location permissions on both devices.
   - The devices will automatically discover each other via Bluetooth / Wi-Fi Direct.
   - Tap **SOS BROADCAST** on one device to test the real-time multi-hop propagation, siren, Morse vibration, and GPS coordinates receipt on the other device!
