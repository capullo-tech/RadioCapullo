# RadioCapullo

![Build status](https://img.shields.io/github/actions/workflow/status/capullo-tech/RadioCapullo/Build.yml?branch=main)
![Latest Release](https://img.shields.io/github/v/release/capullo-tech/RadioCapullo)
![License](https://img.shields.io/github/license/capullo-tech/RadioCapullo)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Minimum Android Version](https://img.shields.io/badge/Min%20Android%20version-6-green)
![Compiled Android SDK Version](https://img.shields.io/badge/Compiled%20SDK%20version-34-green)

**RadioCapullo** is a free, open-source Android application designed for music broadcasting and listening in a synchronized multi-device environment. It leverages a star topology to create an immersive audio experience, allowing multiple Android devices to play music simultaneously, enhancing volume and depth of field.

## Features

- **Dual Functionality**: Acts as both a Snapserver and Snapclient for music broadcasting and listening.
- **Spotify Integration**: Streams Spotify content seamlessly via librespot-java.
- **Multiroom Audio Sync**: Ensures synchronized playback across devices for an immersive listening experience.

## Architecture

RadioCapullo integrates [librespot-java](https://github.com/devgianlu/librespot-java) for Spotify content streaming and [Snapcast](https://github.com/badaix/snapcast) for synchronized multiroom audio playback. This combination allows any Android device to become a Spotify speaker and part of a synchronized audio network.

```
                             +---------------+
                             |  Spotify App  |
                             +-------+-------+
                                     |
                                     | Initiates Playback 
                                     v
                          +---------------------+
                          | Android Device      |
                          | (Server & Client)   |
                          |---------------------|
                          | Server Control UI   |
                          | librespot-java      |
                          | FIFO Queue          |
                          | Snapserver &        |
                          | Snapclient          |
                          | Broadcasts & Plays  |
                          | Audio               |
                          +----------+----------+
                                     | LAN Broadcast
          +--------------------------+-----------------------------+
          |                          |                             |
+---------v---------+      +---------v---------+         +---------v---------+
|  Android Device   |      |  Android Device   |         |  Android Device   |
|    (Client 1)     |      |    (Client 2)     |         |    (Client N)     |
|-------------------|      |-------------------|         |-------------------|
| Client Control UI |      | Client Control UI |  . . .  | Client Control UI |
| Audio Output      |      | Audio Output      |         | Audio Output      |
| (Speakers, Sync   |      | (Speakers, Sync   |         | (Speakers, Sync   |
| via Snapclient)   |      | via Snapclient)   |         | via Snapclient)   |
+------------------ +      +-------------------+         +-------------------+
```

### Server and Client Roles

- **Server**: Broadcasts audio and plays it simultaneously, acting as a Snapclient.
- **Client**: Connects to the server to play the broadcasted audio in sync.

## Getting Started

### Prerequisites

- All devices must be connected to the same network (WLAN, Hotspot).

### Running a Broadcaster

1. Open RadioCapullo and press the Broadcast button.
2. Use a Spotify app to select the device as a speaker and play music.
3. RadioCapullo will start broadcasting and playing the music locally.

### Running a Listener

1. Open RadioCapullo on another device and enter the broadcaster's IP.
2. Click Play to start listening to the synchronized broadcast.

## Contributing

Contributions are welcome! Please fork the repository, make your changes, and submit a pull request.

Clone this repo with `git clone --recurse-submodules` or do `git submodule update --init --recursive` immediately after cloning to initialize all submodules

## Disclaimers
### Fair use of Spotify

> [!IMPORTANT]
> RadioCapullo's Spotify integration via librespot-java is intended for personal use within the bounds of Spotify's terms of service.
> RadioCapullo does not endorse or encourage any form of illicit use or misuse of the application.

## Acknowledgments

- [librespot-java](https://github.com/devgianlu/librespot-java)
- [jetispot](https://github.com/iTaysonLab/jetispot)
- [Snapcast](https://github.com/badaix/snapcast)
