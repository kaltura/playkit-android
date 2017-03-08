package com.kaltura.playkit.player;

import android.support.test.runner.AndroidJUnit4;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class SourceSelectorAndroidTest {

    PKMediaSource dashWidevine = new PKMediaSource()
            .setMediaFormat(PKMediaFormat.dash)
            .setUrl("http://example.com/a.mpd")
            .setDrmData(Collections.singletonList(new PKDrmParams("http://whatever", PKDrmParams.Scheme.WidevineCENC)));
    PKMediaSource dashClear = new PKMediaSource().setMediaFormat(PKMediaFormat.dash).setUrl("http://example.com/a.mpd");
    PKMediaSource mp4 = new PKMediaSource().setMediaFormat(PKMediaFormat.mp4).setUrl("http://example.com/a.mp4");
    PKMediaSource hls = new PKMediaSource().setMediaFormat(PKMediaFormat.hls).setUrl("http://example.com/a.m3u8");
    PKMediaSource wvm = new PKMediaSource().setMediaFormat(PKMediaFormat.wvm).setUrl("http://example.com/a.wvm");
    PKMediaSource mp3 = new PKMediaSource().setMediaFormat(PKMediaFormat.mp3).setUrl("http://example.com/a.mp3");

    private PKMediaEntry entry(PKMediaSource... sources) {
        return new PKMediaEntry().setSources(Arrays.asList(sources));
    }

    @Test
    public void mediaFormatByExtension() throws Exception {
        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfExt("mpd"));
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfExt("mp4"));
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfExt("m3u8"));
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfExt("wvm"));
        assertEquals(null, PKMediaFormat.valueOfExt("foo"));
        assertEquals(null, PKMediaFormat.valueOfExt(null));


        assertEquals(PKMediaFormat.dash, PKMediaFormat.valueOfUrl(dashClear.getUrl()));
        assertEquals(PKMediaFormat.mp4, PKMediaFormat.valueOfUrl(mp4.getUrl()));
        assertEquals(PKMediaFormat.hls, PKMediaFormat.valueOfUrl(hls.getUrl()));
        assertEquals(PKMediaFormat.wvm, PKMediaFormat.valueOfUrl(wvm.getUrl()));
        
    }

    @Test
    public void sourceSelector() {
        
        assertTrue(SourceSelector.selectSource(entry(mp4, dashClear)) == dashClear);
        assertTrue(SourceSelector.selectSource(entry(mp4, hls)) == hls);
        assertTrue(SourceSelector.selectSource(entry(hls, mp4, dashClear)) == dashClear);
        assertTrue(SourceSelector.selectSource(entry(hls, wvm)) == hls);
        assertTrue(SourceSelector.selectSource(entry(dashWidevine, wvm, hls, mp4)) == dashWidevine);
        assertTrue(SourceSelector.selectSource(entry(mp3)) == mp3);
//        assertTrue(SourceSelector.selectSource(entry(mp4, wvm)) == wvm);

    }
}
