# RadioCapullo

## Overview
This is a free and open source Android application with music broadcast and listen dual modes.

**RadioCapullo** creates an atmospheric music environment running in multiple android devices in a star topology. 
All listeners play the music at exactly the same time, increasing the overall volume and the depth of field.


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
It integrates Snapcast's multiroom audio streaming with librespot-java, enabling devices to act both as an audio source and receiver within a multiroom setup. This app transforms any Android device into a Spotify speaker using Zeroconf for device discovery.

```
+---------------------+          +----------------------+
| Android Device      |          | librespot-java     |
| (Server & Client)   |          | Spotify & Zeroconf  |
|---------------------|          |----------------------|
|  Server Control UI  |<-------->|  Network Discovery   |
|  Client Control UI  |          |  FIFO Queue          |
|  FIFO Queue         |----+     +----------------------+
|  Audio Output       |    |
+---------------------+    |             ^
                           |             |
                           |             |
                           |             |
                           v             |
                    +--------------+     |
                    |  Snapcast    |<----+
                    |--------------|
                    | Broadcasts & | 
                    | Plays Audio  |
                    +--------------+
       
       


```
#### Audio Processing and Broadcasting
RadioCapullo utilizes librespot-java to handle Spotify integration, allowing the server device to function as a Spotify speaker. The audio output from librespot-java is directed into a FIFO (First In, First Out) queue, which effectively manages the audio data stream. Snapserver then accesses this FIFO as its input source for broadcasting the audio across the network.

#### Server Playback
Simultaneously, the server device also acts as a Snapclient. It plays the same music it is broadcasting, ensuring that it can both stream and participate in the synchronized multiroom audio experience. This dual functionality allows for real-time monitoring and enjoyment of the streamed content directly on the server device.

These processes ensure a cohesive and synchronized streaming experience across all connected devices within the network, adhering to the principles of fair use and respecting Spotify's terms of service.

### Use case Scenarios
```
Gathered with friends but no loudspeaker?
What if everyone's phone could play in unison. 
Can you imagine what happens if you place the phones around-away from you?
```

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

```
                        +---------------------+
                        | Android Device      |
                        | (Server & Client)   |
                        |---------------------|
                        | Server Control UI   |
                        | Audio Capture       |
                        | FIFO Queue          |
                        | librespot-java      |
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
## Contributing
Contributions are welcome. Please fork the repository, make your changes, and submit a pull request.

## License
RadioCapullo is released under the MIT License. See the LICENSE file in the repository for more details.

## Disclaimers

### Fair Use of Spotify
RadioCapullo integrates with Spotify via librespot-java for streaming audio. This integration is intended solely for personal use within the bounds of Spotify's terms of service. Users are responsible for ensuring their usage complies with all relevant laws and Spotify’s terms.

### No Endorsement for Illicit Use
This project is developed for lawful purposes and should not be used to infringe upon the rights of Spotify or any other party. The developers of RadioCapullo do not endorse or promote any illicit use of this software or any misuse that violates Spotify's terms of service.

Please ensure that your use of RadioCapullo and Spotify complies with legal standards and Spotify's terms of use.

## Acknowledgments
Special thanks to the Snapcast and librespot-java projects for their pioneering technologies which made this app possible.