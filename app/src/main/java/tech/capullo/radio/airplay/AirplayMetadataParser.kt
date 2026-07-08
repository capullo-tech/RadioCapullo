package tech.capullo.radio.airplay

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import tech.capullo.radio.snapcast.ArtData
import tech.capullo.radio.snapcast.StreamMetadata
import tech.capullo.radio.snapcast.StreamProperties

/**
 * Parses shairport-sync's metadata pipe stream (emitted when built with
 * CONFIG_METADATA) into Snapcast [StreamProperties]. Pure / no Android deps so
 * it can be unit-tested; base64 decoding is injected.
 *
 * shairport writes newline-terminated items (see rtsp.c `metadata_process`):
 *
 *     <item><type>HEX</type><code>HEX</code><length>N</length>
 *     <data encoding="base64">
 *     BASE64</data></item>
 *
 * where the `<data>` block is absent for zero-length items, the base64 has no
 * internal newlines, and HEX is the big-endian uint32 of a 4-char code. Codes
 * arrive under two types: `core` (DAAP metadata) and `ssnc` (shairport-sync
 * control). Feed raw pipe text in arbitrary chunks; complete items are parsed
 * and a cumulative snapshot is returned.
 */
class AirplayMetadataParser(private val decodeBase64: (String) -> ByteArray) {

    private val buffer = StringBuilder()

    // Cumulative player state, updated as items arrive.
    private var title: String? = null
    private var artist: String? = null
    private var album: String? = null
    private var durationSec: Float? = null
    private var positionSec: Float = 0f
    private var artDataBase64: String? = null
    private var artExtension: String = "jpg"
    private var volumePercent: Int? = null
    private var muted: Boolean = false

    // null = nothing has played yet (reported as "stopped").
    private var playing: Boolean? = null

    // DACP remote-control credentials from the connected sender (ssnc acre/daid).
    private var activeRemote: String? = null
    private var dacpId: String? = null

    /** DACP credentials, non-null only once both acre and daid have arrived. */
    fun credentials(): DacpCredentials? {
        val ar = activeRemote
        val id = dacpId
        return if (ar != null && id != null) DacpCredentials(id, ar) else null
    }

    /**
     * Feeds a chunk of raw pipe text. Returns a cumulative [StreamProperties]
     * snapshot if at least one complete item was parsed, else null.
     */
    fun feed(text: String): StreamProperties? {
        buffer.append(text)
        var lastEnd = 0
        var changed = false
        for (match in ITEM_REGEX.findAll(buffer)) {
            processItem(match)
            lastEnd = match.range.last + 1
            changed = true
        }
        if (lastEnd > 0) buffer.delete(0, lastEnd)
        return if (changed) snapshot() else null
    }

    /** Clears any partial item left over (e.g. after the writer closed). */
    fun resetBuffer() = buffer.setLength(0)

    fun snapshot(): StreamProperties {
        val controllable = credentials() != null
        val hasMetadata = title != null || artist != null || album != null ||
            artDataBase64 != null || durationSec != null
        val metadata = if (hasMetadata) {
            StreamMetadata(
                album = album,
                artist = artist?.let { JsonArray(listOf(JsonPrimitive(it))) },
                track = null,
                title = title,
                duration = durationSec,
                artUrl = null,
                artData = artDataBase64?.let { ArtData(data = it, extension = artExtension) },
            )
        } else {
            null
        }
        return StreamProperties(
            playbackStatus = when (playing) {
                true -> "playing"
                false -> "paused"
                null -> "stopped"
            },
            loopStatus = "none",
            shuffle = false,
            volume = volumePercent,
            mute = muted,
            rate = 1.0f,
            position = positionSec,
            // Controls are advertised only once we hold DACP credentials, i.e.
            // the sender is reachable for transport commands. Seek is unsupported.
            canControl = controllable,
            canGoNext = controllable,
            canGoPrevious = controllable,
            canPause = controllable,
            canPlay = controllable,
            canSeek = false,
            metadata = metadata,
        )
    }

    private fun processItem(match: MatchResult) {
        val type = fourCc(match.groupValues[1])
        val code = fourCc(match.groupValues[2])
        val data = match.groups[4]?.value

        when (type) {
            "core" -> when (code) {
                "minm" -> title = data?.let { decodeText(it) }
                "asar" -> artist = data?.let { decodeText(it) }
                "asal" -> album = data?.let { decodeText(it) }
                "astm" -> data?.let { durationSec = decodeBigEndianInt(it) / 1000f }
            }

            "ssnc" -> when (code) {
                "PICT" -> if (data != null && data.isNotBlank()) {
                    artDataBase64 = data
                    artExtension = detectImageExtension(data)
                }

                // Begin / resume -> playing; flush (pause) / end (stop) -> not.
                "pbeg", "prsm" -> playing = true

                "pfls", "pend" -> playing = false

                "prgr" -> data?.let { parseProgress(decodeText(it)) }

                "pvol" -> data?.let { parseVolume(decodeText(it)) }

                // DACP remote-control credentials for driving the sender.
                "acre" -> activeRemote = data?.let { decodeText(it) }

                "daid" -> dacpId = data?.let { decodeText(it) }
            }
        }
    }

    private fun decodeText(base64: String): String =
        String(decodeBase64(base64), Charsets.UTF_8).trim()

    private fun decodeBigEndianInt(base64: String): Int {
        val bytes = decodeBase64(base64)
        var value = 0
        for (b in bytes) value = (value shl 8) or (b.toInt() and 0xFF)
        return value
    }

    // "start/current/end" in RTP frames (44100 Hz). Yields position & duration.
    private fun parseProgress(text: String) {
        val parts = text.split('/')
        if (parts.size != 3) return
        val start = parts[0].trim().toLongOrNull() ?: return
        val current = parts[1].trim().toLongOrNull() ?: return
        val end = parts[2].trim().toLongOrNull() ?: return
        positionSec = ((current - start).coerceAtLeast(0)) / RTP_RATE
        if (end > start) durationSec = (end - start) / RTP_RATE
    }

    // "airplay_volume,volume,lowest,highest"; airplay_volume is -30..0 dB, or
    // <= -144 for mute. Mapped linearly to 0..100 for display only.
    private fun parseVolume(text: String) {
        val airplayVolume = text.split(',').firstOrNull()?.trim()?.toFloatOrNull() ?: return
        if (airplayVolume <= MUTE_THRESHOLD_DB) {
            muted = true
        } else {
            muted = false
            volumePercent = (((airplayVolume + VOLUME_RANGE_DB) / VOLUME_RANGE_DB) * 100f)
                .toInt().coerceIn(0, 100)
        }
    }

    // Detect from the base64 prefix of the image's magic bytes:
    // PNG 0x89 'P' 'N' 'G' -> "iVBOR"; JPEG 0xFF 0xD8 0xFF -> "/9j/".
    private fun detectImageExtension(base64: String): String = when {
        base64.startsWith("iVBOR") -> "png"
        base64.startsWith("/9j/") -> "jpg"
        else -> "jpg"
    }

    private fun fourCc(hex: String): String {
        val v = hex.toLong(16)
        return charArrayOf(
            ((v shr 24) and 0xFF).toInt().toChar(),
            ((v shr 16) and 0xFF).toInt().toChar(),
            ((v shr 8) and 0xFF).toInt().toChar(),
            (v and 0xFF).toInt().toChar(),
        ).concatToString()
    }

    companion object {
        private const val RTP_RATE = 44100f
        private const val MUTE_THRESHOLD_DB = -144f
        private const val VOLUME_RANGE_DB = 30f

        private val ITEM_REGEX = Regex(
            "<item><type>([0-9a-fA-F]+)</type><code>([0-9a-fA-F]+)</code>" +
                "<length>(\\d+)</length>" +
                "(?:\\n<data encoding=\"base64\">\\n(.*?)</data>)?</item>",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}
