![RadioCapullo](https://avatars.githubusercontent.com/u/152348732)

This is a free and opensource **WiFi Radio** for Android 6+, with broadcast and listen dual modes.
**RadioCapullo** creates an atmospheric music environment running in multiple android devices in a star topology. 
All listeners play the music at exactly the same time, increasing the overall volume and the depth of field.

```
Gathered with friends but no loudspeaker?
What if everyone's phone could play in unison. 
Can you imagine what happens if you place the phones around-away from you?
```

Spatial audio: The beggining

>3D audio effects are a group of sound effects that manipulate the sound produced by stereo speakers. This frequently involves the virtual placement of sound sources anywhere in three-dimensional space, including behind, above or below the listener.
>3-D audio (processing) is the spatial domain convolution of sound waves using Head-related transfer functions. It is the phenomenon of transforming sound waves (using head-related transfer function or HRTF filters and cross talk cancellation techniques) to mimic natural sounds waves, which emanate from a point in a 3-D space. It allows trickery of the brain using the ears and auditory nerves, pretending to place different sounds in different 3-D locations upon hearing the sounds.

 - We can control the volume of each listener remotely, this allows us to create some movement effect in the music (ex: spin, vaiven, jump)
 - For now we are limited to simultanueous L+R playback on each listener. 
 - But the goal is to selectively play (monoL) or (monoR) audio channel in each listener by command. Grouping listeners in a specific audio channel & mapping the physical location in the room. This will allow us to create Real Stereo and manipulate the depth of field.


*Listener mode*
 - Default.
 - Just open the app, and it will start listening/playing to the last broadcaster IP if available.

*Broadcast mode*
 - Button to start advertising as spotfi-speaker to any spotfi-app in the network.
 - After its activated by any spotfi-app (including same phone), it will start playing its own broadcast.


**Steps to use:**
 1. Make sure all devices are connected to same network SSID (WLAN, Hotspot) 

Run a broadcaster
 - Open RadioCapullo and press Broadcast button to start advertising the phone's speaker as [spotfi-speaker], labeled using phone's name.
 - Use a spotfi-app in same or any device from network, to scan & control [phone-spotfi-speaker]. Play any song in spotfi-app.
 - When RadioCapullo detects it has been selected as a spotfi-speaker, it starts the local WiFi broadcast and playback. Play any song in spotfi-app.
  
Run a listener
 - Use any android device from network to playback the transmission OTA. open RadioCapullo and write the broadcaster IP > click Play. Device starts playing synchronous music.
 - Any radio listener can change the music queue. Use spotfi-app to scan & JAM the broadcaster's [spotfi-speaker]. Play any song in spotfi-app.
