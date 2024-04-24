# RadioCapullo

## Overview
RadioCapullo is an Android application that integrates Snapcast's multiroom audio streaming with librespot-java, enabling devices to act both as an audio source and receiver within a multiroom setup. This app transforms any Android device into a Spotify speaker using Zeroconf for device discovery.

## Features
- **Dual Functionality**: Functions as both a Snapserver and Snapclient.
- **Spotify Integration**: Streams Spotify content via librespot-java.
- **Multiroom Audio Sync**: Ensures synchronized playback across multiple devices.

## Installation
1. Clone the repository.
2. Open with Android Studio.
3. Build and install on your Android device.

## Architecture
### App Components
```
+---------------------+          +----------------------+
| Android Device      |          | librespot-java     |
| (Server & Client)   |          | Spotify & Zeroconf  |
|---------------------|          |----------------------|
|  Server Control UI  |<-------->|  Network Discovery   |
|  Client Control UI  |          |  FIFO Queue          |
|  FIFO Queue         |----+     +----------------------+
+---------------------+    |             ^
       ^                   |             |
       |                   |             |
       |                   |             |
       |                   v             |
       |            +------------+       |
       |            | Snapserver |<------+
       |            | Broadcasts | 
       |            |   Audio    |
       |            +------------+
       |
       v
+---------------------+
| Android Device      |
|      (Client)       |
|---------------------|
|  Client Control UI  |
|  Audio Output       |
|  (Speakers)         |
+---------------------+

```
The architecture of RadioCapullo involves two main components: an Android device configured as both a server and a client, and the librespot-java integration. The server side includes a server control UI, audio capture, a FIFO queue for managing audio data, and integration with librespot-java for Spotify connectivity and Zeroconf network discovery. The unique feature of the server setup is that it also acts as its own client, allowing it to play the same music it broadcasts. This dual capability ensures that the server device participates as an active player in the synchronized multiroom audio experience, seamlessly integrating into the environment as both a source and receiver of the audio stream.



### Use case Scenarios
```
                        +---------------------+
                        | Android Device      |
                        | (Server & Client)   |
                        |---------------------|
                        | Server Control UI   |
                        | Audio Capture       |
                        | FIFO Queue          |
                        | librespout-java      |
                        | Snapserver &        |
                        | Snapclient          |
                        | Broadcasts & Plays  |
                        | Audio               |
                        +----------+----------+
                                   |
         +-------------------------+-------------------------+
         |                         |                         |
+--------v---------+    +---------v--------+    +-----------v-------+
| Android Device   |    | Android Device   |    | Android Device    |
|     (Client 1)   |    |     (Client 2)   |    |     (Client N)    |
|------------------|    |------------------|    |-------------------|
| Client Control UI|    | Client Control UI|    | Client Control UI |
| Audio Output     |    | Audio Output     |    | Audio Output      |
| (Speakers, Sync  |    | (Speakers, Sync  |    | (Speakers, Sync  |
| via Snapclient)  |    | via Snapclient)  |    | via Snapclient)  |
+------------------+    +------------------+    +-------------------+
```
## Updated Use Case Scenarios
Below are the use case scenarios that align with the ASCII diagrams provided, detailing both the server's dual functionality and the typical client setup:

1. **Server as a Broadcast and Playback Unit**: 
   - The Android device operates as a server using the `Server Control UI` to manage audio capture and processing through a `FIFO Queue`.
   - It employs `librespot-java` for Spotify integration, appearing on the network via Zeroconf.
   - As a Snapserver, it broadcasts the audio across the network.
   - Simultaneously, it acts as its own Snapclient, playing the broadcasted audio locally for immediate feedback and ensuring the audio is in sync.

2. **Clients Receiving Broadcast**:
   - Multiple client devices connect to the server.
   - Each client uses its `Client Control UI` to tune into the server's broadcast.
   - Audio is output through each client's speakers, synchronized across all devices via Snapclient, ensuring uniform multiroom audio playback.

These scenarios illustrate how RadioCapullo facilitates a comprehensive audio streaming experience, allowing devices to serve as both sources and receivers in a synchronized audio environment. This setup enhances the flexibility and usability of home audio systems.

## Contributing
Contributions are welcome. Please fork the repository, make your changes, and submit a pull request.

## License
RadioCapullo is released under the MIT License. See the LICENSE file in the repository for more details.

## Acknowledgments
Special thanks to the Snapcast and librespot-java projects for their pioneering technologies which made this app possible.