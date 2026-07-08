package tech.capullo.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.capullo.radio.airplay.AirplayMetadataParser
import java.util.Base64

class AirplayMetadataParserTest {

    private fun parser() = AirplayMetadataParser { Base64.getDecoder().decode(it) }

    // Builds one shairport-sync metadata item exactly as rtsp.c metadata_process
    // emits it (see the format note in AirplayMetadataParser).
    private fun item(type: String, code: String, value: ByteArray?): String {
        val typeHex = fourCcHex(type)
        val codeHex = fourCcHex(code)
        return if (value == null) {
            "<item><type>$typeHex</type><code>$codeHex</code><length>0</length></item>\n"
        } else {
            val b64 = Base64.getEncoder().encodeToString(value)
            "<item><type>$typeHex</type><code>$codeHex</code><length>${value.size}</length>\n" +
                "<data encoding=\"base64\">\n$b64</data></item>\n"
        }
    }

    private fun textItem(type: String, code: String, value: String) =
        item(type, code, value.toByteArray(Charsets.UTF_8))

    private fun fourCcHex(s: String): String {
        var v = 0L
        for (c in s) v = (v shl 8) or c.code.toLong()
        return v.toString(16)
    }

    @Test
    fun parsesTitleArtistAlbumAndPlayState() {
        val p = parser()
        p.feed(textItem("core", "minm", "My Song"))
        p.feed(textItem("core", "asar", "My Artist"))
        p.feed(textItem("core", "asal", "My Album"))
        val props = p.feed(item("ssnc", "pbeg", null))

        assertNotNull(props)
        assertEquals("playing", props!!.playbackStatus)
        assertEquals("My Song", props.metadata?.title)
        assertEquals("My Album", props.metadata?.album)
        assertTrue(props.metadata?.artist.toString().contains("My Artist"))
        // Phase 1 read-only: controls advertised off.
        assertEquals(false, props.canControl)
        assertEquals(false, props.canGoNext)
    }

    @Test
    fun pendMarksPaused() {
        val p = parser()
        p.feed(item("ssnc", "pbeg", null))
        val props = p.feed(item("ssnc", "pend", null))
        assertEquals("paused", props!!.playbackStatus)
    }

    @Test
    fun stoppedBeforeAnyPlayState() {
        val props = parser().feed(textItem("core", "minm", "Song"))
        assertEquals("stopped", props!!.playbackStatus)
    }

    @Test
    fun decodesDurationFromAstmMillis() {
        val durationMs = 215_000
        val bytes = byteArrayOf(
            (durationMs shr 24 and 0xFF).toByte(),
            (durationMs shr 16 and 0xFF).toByte(),
            (durationMs shr 8 and 0xFF).toByte(),
            (durationMs and 0xFF).toByte(),
        )
        val props = parser().feed(item("core", "astm", bytes))
        assertEquals(215.0f, props!!.metadata?.duration!!, 0.001f)
    }

    @Test
    fun parsesProgressIntoPositionAndDuration() {
        // start=44100, current=132300, end=8864100 (RTP frames @ 44100 Hz)
        val props = parser().feed(textItem("ssnc", "prgr", "44100/132300/8864100"))
        assertEquals(2.0f, props!!.position!!, 0.001f)
        assertEquals(200.0f, props.metadata?.duration ?: 0f, 0.5f)
    }

    @Test
    fun detectsJpegCoverArtExtension() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
        val props = parser().feed(item("ssnc", "PICT", jpeg))
        assertEquals("jpg", props!!.metadata?.artData?.extension)
        assertNotNull(props.metadata?.artData?.data)
    }

    @Test
    fun detectsPngCoverArtExtension() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val props = parser().feed(item("ssnc", "PICT", png))
        assertEquals("png", props!!.metadata?.artData?.extension)
    }

    @Test
    fun handlesItemSplitAcrossFeeds() {
        val full = textItem("core", "minm", "Split Title")
        val cut = full.length / 2
        val p = parser()
        assertNull(p.feed(full.substring(0, cut)))
        val props = p.feed(full.substring(cut))
        assertEquals("Split Title", props!!.metadata?.title)
    }

    @Test
    fun mapsVolumeAndMute() {
        val p = parser()
        // airplay_volume 0 dB -> 100%
        var props = p.feed(textItem("ssnc", "pvol", "0.0,0.0,-30.0,0.0"))
        assertEquals(100, props!!.volume)
        assertEquals(false, props.mute)
        // -144 -> muted
        props = p.feed(textItem("ssnc", "pvol", "-144.0,-144.0,-30.0,0.0"))
        assertEquals(true, props!!.mute)
    }

    @Test
    fun controlEnabledOnceDacpCredentialsPresent() {
        val p = parser()
        // Read-only until DACP credentials arrive.
        var props = p.feed(item("ssnc", "pbeg", null))
        assertEquals(false, props!!.canControl)

        p.feed(textItem("ssnc", "acre", "3105466202"))
        props = p.feed(textItem("ssnc", "daid", "DF6FDF152E3705EF"))
        assertEquals(true, props!!.canControl)
        assertEquals(true, props.canGoNext)
        assertEquals(true, props.canGoPrevious)
        assertEquals(true, props.canPause)
        assertEquals(true, props.canPlay)
        // Seek is never supported over DACP in this integration.
        assertEquals(false, props.canSeek)
    }

    @Test
    fun controlStaysOffWithOnlyActiveRemote() {
        val props = parser().feed(textItem("ssnc", "acre", "3105466202"))
        assertEquals(false, props!!.canControl)
    }
}
