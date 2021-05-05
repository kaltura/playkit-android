package com.kaltura.playkit.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.playkit.PKDrmParams
import com.kaltura.playkit.PKMediaEntry
import com.kaltura.playkit.PKMediaFormat
import com.kaltura.playkit.PKMediaSource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceSelectorTest {
    val MPD_EXAMPLE = "http://example.com/a.mpd"
    val MP4_EXAMPLE = "http://example.com/a.mp4"
    val M3U8_EXAMPLE = "http://example.com/a.m3u8"
    val WVM_EXAMPLE = "http://example.com/a.wvm"
    val MP3_EXAMPLE = "http://example.com/a.mp3"

    var dashWidevine = PKMediaSource()
            .setMediaFormat(PKMediaFormat.dash)
            .setUrl("http://example.com/a.mpd")
            .setDrmData(listOf(PKDrmParams("http://whatever", PKDrmParams.Scheme.WidevineCENC)))

    var dashClear = PKMediaSource().setMediaFormat(PKMediaFormat.dash).setUrl(MPD_EXAMPLE)
    var mp4 = PKMediaSource().setMediaFormat(PKMediaFormat.mp4).setUrl(MP4_EXAMPLE)
    var hls = PKMediaSource().setMediaFormat(PKMediaFormat.hls).setUrl(M3U8_EXAMPLE)
    var wvm = PKMediaSource().setMediaFormat(PKMediaFormat.wvm).setUrl(WVM_EXAMPLE)
    var mp3 = PKMediaSource().setMediaFormat(PKMediaFormat.mp3).setUrl(MP3_EXAMPLE)

    private fun entry(vararg sources: PKMediaSource): PKMediaEntry? {
        return PKMediaEntry().setSources(listOf(*sources))
    }

    @Test
    @Throws(Exception::class)
    fun mediaFormatByExtension() {
        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfExt("mpd"))
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfExt("mp4"))
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfExt("m3u8"))
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfExt("wvm"))
        assertNull(null, PKMediaFormat.valueOfExt("foo"))
        assertEquals(PKMediaFormat.udp, PKMediaFormat.valueOfExt(null))
        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfUrl(dashClear.url))
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfUrl(mp4.url))
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfUrl(hls.url))
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfUrl(wvm.url))
        assertNull(null, PKMediaFormat.valueOfUrl(null))
    }

    @Test
    fun sourceSelector() {
        assertTrue(SourceSelector.selectSource(entry(mp4, dashClear), PKMediaFormat.dash) === dashClear)
        assertTrue(SourceSelector.selectSource(entry(mp4, hls), PKMediaFormat.hls) === hls)
        assertTrue(SourceSelector.selectSource(entry(hls, mp4, dashClear), PKMediaFormat.dash) === dashClear)
        assertTrue(SourceSelector.selectSource(entry(hls, wvm), PKMediaFormat.dash) === hls)
        assertFalse(SourceSelector.selectSource(entry(dashWidevine, wvm, hls, mp4), PKMediaFormat.wvm) === dashWidevine)
        assertTrue(SourceSelector.selectSource(entry(mp3), PKMediaFormat.mp3) === mp3)
        assertTrue(SourceSelector.selectSource(entry(mp4, wvm), PKMediaFormat.hls) === wvm)
    }
}