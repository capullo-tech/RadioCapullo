package tech.capullo.radio


class PlaybackService {}/*: MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // Create your player and media session in the onCreate lifecycle event
    @OptIn(UnstableApi::class) override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun play() {
                // Add custom logic
                Log.d("PLAYBACKSERVICE", "onPlay")
                super.play()
            }

            override fun setPlayWhenReady(playWhenReady: Boolean) {
                // Add custom logic
                super.setPlayWhenReady(playWhenReady)
            }

            override fun addMediaItem(mediaItem: MediaItem) {

                Log.d("PLAYBACKSERVICE", "adding media item:$mediaItem")
                super.addMediaItem(mediaItem)
            }
        }


        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

        */