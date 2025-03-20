package tech.capullo.radio.espoti

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import xyz.gianlu.librespot.player.decoders.Decoder
import xyz.gianlu.librespot.player.decoders.SeekableInputStream
import xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

class AndroidNativeDecoder(
    audioIn: SeekableInputStream,
    normalizationFactor: Float,
    duration: Int,
) : Decoder(audioIn, normalizationFactor, duration) {
    private val buffer = ByteArray(2 * BUFFER_SIZE)
    private val codec: MediaCodec
    private val extractor: MediaExtractor
    private val closeLock = Any()
    private var presentationTime: Long = 0

    init {
        val start = audioIn.position()
        extractor = MediaExtractor()
        extractor.setDataSource(object : MediaDataSource() {
            @Throws(IOException::class)
            override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                audioIn.seek(position.toInt() + start)
                return audioIn.read(buffer, offset, size)
            }

            override fun getSize(): Long = (audioIn.size() - start).toLong()

            override fun close() {
                audioIn.close()
            }
        })

        if (extractor.trackCount == 0) throw DecoderException("No tracks found.")

        extractor.selectTrack(0)

        val format = extractor.getTrackFormat(0)

        codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var sampleSize = 16
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sampleSize = when (
                format.getInteger(
                    MediaFormat.KEY_PCM_ENCODING,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            ) {
                AudioFormat.ENCODING_PCM_8BIT -> 8
                AudioFormat.ENCODING_PCM_16BIT -> 16
                else -> throw DecoderException("Unsupported PCM encoding.")
            }
        }

        audioFormat = OutputAudioFormat(
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toFloat(),
            sampleSize,
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            true,
            false,
        )
    }

    @Throws(IOException::class)
    override fun readInternal(out: OutputStream): Int {
        val info = MediaCodec.BufferInfo()

        while (true) {
            if (closed) return -1

            synchronized(closeLock) {
                val inputBufferId = codec.dequeueInputBuffer(-1)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    val count = extractor.readSampleData(inputBuffer!!, 0)
                    if (count == -1) {
                        try {
                            codec.signalEndOfInputStream()
                        } catch (ex: IllegalStateException) {
                            Log.e(TAG, "Failed signaling end of input stream.", ex)
                            return -1
                        }
                        return -1
                    }
                    codec.queueInputBuffer(
                        inputBufferId,
                        inputBuffer.position(),
                        inputBuffer.limit(),
                        extractor.sampleTime,
                        0,
                    )
                    extractor.advance()
                }

                val outputBufferId = codec.dequeueOutputBuffer(info, 10000)
                if (outputBufferId >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)

                    while (outputBuffer!!.remaining() > 0) {
                        val read =
                            min(outputBuffer.remaining().toDouble(), buffer.size.toDouble()).toInt()
                        outputBuffer[buffer, 0, read]
                        out.write(buffer, 0, read)
                    }

                    codec.releaseOutputBuffer(outputBufferId, false)
                    presentationTime = TimeUnit.MICROSECONDS.toMillis(info.presentationTimeUs)
                    return info.size
                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d(TAG, "Timeout")
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "Output format changed: " + codec.outputFormat)
                } else {
                    Log.e(
                        TAG,
                        "Failed decoding: $outputBufferId",
                    )
                    return -1
                }
            }
        }
    }

    override fun seek(positionMs: Int) {
        extractor.seekTo(
            TimeUnit.MILLISECONDS.toMicros(positionMs.toLong()),
            MediaExtractor.SEEK_TO_CLOSEST_SYNC,
        )
    }

    @Throws(IOException::class)
    override fun close() {
        synchronized(closeLock) {
            codec.release()
            extractor.release()
            super.close()
        }
    }

    override fun time(): Int = presentationTime.toInt()

    companion object {
        private val TAG: String = AndroidNativeDecoder::class.java.simpleName
    }
}
