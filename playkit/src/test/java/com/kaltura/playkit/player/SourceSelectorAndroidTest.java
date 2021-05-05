package com.kaltura.playkit.player;

import android.net.Uri;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SourceSelectorAndroidTest {

    public static final String MPD_EXAMPLE = "http://example.com/a.mpd";
    public static final String MP4_EXAMPLE = "http://example.com/a.mp4";
    public static final String M3U8_EXAMPLE = "http://example.com/a.m3u8";
    public static final String WVM_EXAMPLE = "http://example.com/a.wvm";
    public static final String MP3_EXAMPLE = "http://example.com/a.mp3";


    PKMediaSource dashWidevine = new PKMediaSource()
            .setMediaFormat(PKMediaFormat.dash)
            .setUrl("http://example.com/a.mpd")
            .setDrmData(Collections.singletonList(new PKDrmParams("http://whatever", PKDrmParams.Scheme.WidevineCENC)));
    PKMediaSource dashClear = new PKMediaSource().setMediaFormat(PKMediaFormat.dash).setUrl(MPD_EXAMPLE);
    PKMediaSource mp4 = new PKMediaSource().setMediaFormat(PKMediaFormat.mp4).setUrl(MP4_EXAMPLE);
    PKMediaSource hls = new PKMediaSource().setMediaFormat(PKMediaFormat.hls).setUrl(M3U8_EXAMPLE);
    PKMediaSource wvm = new PKMediaSource().setMediaFormat(PKMediaFormat.wvm).setUrl(WVM_EXAMPLE);
    PKMediaSource mp3 = new PKMediaSource().setMediaFormat(PKMediaFormat.mp3).setUrl(MP3_EXAMPLE);

    private PKMediaEntry entry(PKMediaSource... sources) {
        return new PKMediaEntry().setSources(Arrays.asList(sources));
    }

    @Test
    public void mediaFormatByExtension() throws Exception {
        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfExt("mpd"));
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfExt("mp4"));
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfExt("m3u8"));
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfExt("wvm"));
        assertNull(null, PKMediaFormat.valueOfExt("foo"));
        assertEquals(PKMediaFormat.udp, PKMediaFormat.valueOfExt(null));

        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfUrl(dashClear.getUrl()));
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfUrl(mp4.getUrl()));
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfUrl(hls.getUrl()));
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfUrl(wvm.getUrl()));
        assertNull(null, PKMediaFormat.valueOfUrl(null));
    }

    @Test
    public void sourceSelector() {
        assertTrue(SourceSelector.selectSource(entry(mp4, dashClear), PKMediaFormat.dash) == dashClear);
        assertTrue(SourceSelector.selectSource(entry(mp4, hls), PKMediaFormat.hls) == hls);
        assertTrue(SourceSelector.selectSource(entry(hls, mp4, dashClear), PKMediaFormat.dash) == dashClear);
        assertTrue(SourceSelector.selectSource(entry(hls, wvm), PKMediaFormat.dash) == hls);
        assertFalse(SourceSelector.selectSource(entry(dashWidevine, wvm, hls, mp4), PKMediaFormat.wvm) == dashWidevine);
        assertTrue(SourceSelector.selectSource(entry(mp3), PKMediaFormat.mp3) == mp3);
        assertTrue(SourceSelector.selectSource(entry(mp4, wvm), PKMediaFormat.hls) == wvm);
    }
}
