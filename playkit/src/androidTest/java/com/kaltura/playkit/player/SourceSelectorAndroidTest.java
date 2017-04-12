package com.kaltura.playkit.player;

import android.support.test.runner.AndroidJUnit4;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class SourceSelectorAndroidTest {

    PKMediaSource dashWidevine = new PKMediaSource().setMediaFormat(PKMediaFormat.dash_drm).setUrl("http://example.com/playManifest/a.mpd");
    PKMediaSource dashClear = new PKMediaSource().setMediaFormat(PKMediaFormat.dash_clear).setUrl("http://example.com/playManifest/a.mpd");
    PKMediaSource mp4 = new PKMediaSource().setMediaFormat(PKMediaFormat.mp4_clear).setUrl("http://example.com/a.mp4");
    PKMediaSource hls = new PKMediaSource().setMediaFormat(PKMediaFormat.hls_clear).setUrl("http://example.com/a.m3u8");
    PKMediaSource wvm = new PKMediaSource().setMediaFormat(PKMediaFormat.wvm_widevine).setUrl("http://example.com/a.wvm");

    private PKMediaEntry entry(PKMediaSource... sources) {
        return new PKMediaEntry().setSources(Arrays.asList(sources));
    }

    @Test
    public void mediaFormatByExtension() throws Exception {
        assertEquals(PKMediaFormat.dash_clear, PKMediaFormat.valueOfExt("mpd"));
        assertEquals(PKMediaFormat.mp4_clear, PKMediaFormat.valueOfExt("mp4"));
        assertEquals(PKMediaFormat.hls_clear, PKMediaFormat.valueOfExt("m3u8"));
        assertEquals(PKMediaFormat.wvm_widevine, PKMediaFormat.valueOfExt("wvm"));
        assertEquals(null, PKMediaFormat.valueOfExt("foo"));
        assertEquals(null, PKMediaFormat.valueOfExt(null));


        assertEquals(PKMediaFormat.dash_clear, PKMediaFormat.valueOfUrl(dashClear.getUrl()));
        assertEquals(PKMediaFormat.mp4_clear, PKMediaFormat.valueOfUrl(mp4.getUrl()));
        assertEquals(PKMediaFormat.hls_clear, PKMediaFormat.valueOfUrl(hls.getUrl()));
        assertEquals(PKMediaFormat.wvm_widevine, PKMediaFormat.valueOfUrl(wvm.getUrl()));

    }

    @Test
    public void sourceSelector() {

        assertTrue(SourceSelector.selectSource(entry(mp4, dashClear), "1234") == dashClear);
        String in = SourceSelector.selectSource(entry(mp4, dashClear), "1234").getUrl();
        int numOfOccurance = 0;
        Pattern p = Pattern.compile("playSessionId");
        Matcher m = p.matcher( in );
        while (m.find()) {
            numOfOccurance++;
        }
        assertTrue(numOfOccurance == 1);

        assertTrue(SourceSelector.selectSource(entry(mp4, hls), "1234") == hls);
        assertTrue(SourceSelector.selectSource(entry(hls, mp4, dashClear), "1234") == dashClear);
        assertTrue(SourceSelector.selectSource(entry(hls, wvm) , "1234") == hls);
//        assertTrue(SourceSelector.selectSource(entry(mp4, wvm)) == wvm);

    }
}
