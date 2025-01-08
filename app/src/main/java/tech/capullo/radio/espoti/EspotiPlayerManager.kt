package tech.capullo.radio.espoti

import android.os.Looper
import tech.capullo.radio.data.RadioRepository
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.apply

@Singleton
class EspotiPlayerManager @Inject constructor(
    private val espotiSessionManager: EspotiSessionManager,
    private val radioRepository: RadioRepository,
) {
    @Volatile
    private var _player: Player? = null

    fun playerNullable() = _player

    fun createPlayer() {
        if (_player != null) return
        _player = verifyNotMainThread {
            Player(
                PlayerConfiguration.Builder().apply {
                    setOutput(PlayerConfiguration.AudioOutput.PIPE)
                    setOutputPipe(File(radioRepository.getPipeFilepath()!!))

                    setAutoplayEnabled(true)
                }.build(),
                espotiSessionManager.session
            )
        }
    }

    val player: Player get() = _player ?: error("Player not yet created!")

    private fun <T> verifyNotMainThread(block: () -> T): T {
        if (Looper.getMainLooper() == Looper.myLooper())
            throw IllegalStateException("This should be run only on the non-UI thread!")
        return block()
    }
}
