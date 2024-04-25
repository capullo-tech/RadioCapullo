# RadioCapullo

## Overview
This is a free and open source Android application with music broadcast and listen dual modes.

**RadioCapullo** creates an atmospheric music environment running in multiple android devices in a star topology. 
All listeners play the music at exactly the same time, increasing the overall volume and the depth of field.


## Features
- **Dual Functionality**: Functions as both a Snapserver and Snapclient.
- **Spotify Integration**: Streams Spotify content via librespot-java.
- **Multiroom Audio Sync**: Ensures synchronized playback across multiple devices.

## Architecture
### App Components

```
+---------------------------------------------+
|              RadioCapullo (App)             |
|---------------------------------------------|
| +-----------------+  +--------------------+ |
| | librespot-java  |  | Snapcast           | |
| |-----------------|  |--------------------| |
| | Spotify Network |  | Broadcasts & Plays | |
| | Discovery       |  | Audio              | |
| +-----------------+  +--------------------+ |
+---------------------------------------------+
            ^
            | Initiates Playback
            |
 +-----------------------+
 | External Spotify App  |
 +-----------------------+


```
#### Audio Processing and Broadcasting
RadioCapullo utilizes librespot-java to handle Spotify integration, allowing the server device to function as a Spotify speaker. This app transforms any Android device into a Spotify speaker using Zeroconf for device discovery. The audio output from librespot-java is directed into a FIFO (First In, First Out) queue, which effectively manages the audio data stream. Snapserver then accesses this FIFO as its input source for broadcasting the audio across the network.

#### Server Playback
Simultaneously, the server device also acts as a Snapclient. It plays the same music it is broadcasting, ensuring that it can both stream and participate in the synchronized multiroom audio experience. This dual functionality allows for real-time monitoring and enjoyment of the streamed content directly on the server device.

These processes ensure a cohesive and synchronized streaming experience across all connected devices within the network, adhering to the principles of fair use and respecting Spotify's terms of service.

## Use case Scenarios

- Gathered with friends but no loudspeaker?
- What if everyone's phone could play in unison. 
- Can you imagine what happens if you place the phones around-away from you?

These scenarios illustrate how RadioCapullo facilitates a comprehensive audio streaming experience, allowing devices to serve as both sources and receivers in a synchronized audio environment.

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
                                   | LAN Broadcast
         +-------------------------+-------------------------+
         |                         |                         |
+--------v---------+     +---------v--------+    +-----------v-------+
| Android Device   |     | Android Device   |    | Android Device    |
|     (Client 1)   |     |     (Client 2)   |    |     (Client N)    |
|------------------|     |------------------|    |-------------------|
| Client Control UI|     | Client Control UI|    | Client Control UI |
| Audio Output     |     | Audio Output     |    | Audio Output      |
| (Speakers, Sync  |     | (Speakers, Sync  |    | (Speakers, Sync  |
| via Snapclient)  |     | via Snapclient)  |    | via Snapclient)  |
+------------------+     +------------------+    +-------------------+
```

*Listener mode*
 - Default.
 - Just open the app, and it will start listening/playing to the last broadcaster IP if available.

*Broadcast mode*
 - Button to start advertising as Spotify speaker to any Spotify app in the network.
 - After its activated by any Spotify app (including same phone), it will start playing its own broadcast.


### Steps to use
 1. Make sure all devices are connected to same network SSID (WLAN, Hotspot) 

Run a broadcaster
 - Open RadioCapullo and press Broadcast button to start advertising the phone as [phone-spotify-speaker], labeled using phone's name.
 - Use a Spotify app in same or any device from network, to scan & control [phone-spotify-speaker]. Play any song in spotfi-app.
 - When RadioCapullo detects it has been selected as a Spotify speaker, it starts the local network broadcast and playback. Play any song in Spotify app.

Run a listener
 - Use any android device from network to playback the transmission OTA. open RadioCapullo and write the broadcaster IP > click Play. Device starts playing synchronous music.
 - Any radio listener can change the music queue. Use spotfi-app to scan & JAM the broadcaster's [spotify-speaker]. Play any song in Spotify app.>

## Contributing
Contributions are welcome. Please fork the repository, make your changes, and submit a pull request.

## Disclaimers

### Fair Use of Spotify
RadioCapullo integrates with Spotify via librespot-java for streaming audio. This integration is intended solely for personal use within the bounds of Spotify's terms of service. Users are responsible for ensuring their usage complies with all relevant laws and Spotify’s terms.

### No Endorsement for Illicit Use
This project is developed for lawful purposes and should not be used to infringe upon the rights of Spotify or any other party. The developers of RadioCapullo do not endorse or promote any illicit use of this software or any misuse that violates Spotify's terms of service.

Please ensure that your use of RadioCapullo and Spotify complies with legal standards and Spotify's terms of use.

## Acknowledgments
Special thanks to the Snapcast and librespot-java projects for their pioneering technologies which made this app possible.

## License
RadioCapullo is released under the MIT License. See the LICENSE file in the repository for more details.
