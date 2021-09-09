package com.kaltura.playkit.player.thumbnail;

import com.kaltura.android.exoplayer2.text.Subtitle;
import com.kaltura.android.exoplayer2.text.SubtitleDecoderException;
import com.kaltura.playkit.Utils;

import java.io.IOException;
import java.util.concurrent.Callable;

public class VttThumbnailDownloader implements Callable<Subtitle> {
    private final String vttThumbnailUrl;

    public VttThumbnailDownloader(String vttThumbnailUrl) {
        this.vttThumbnailUrl = vttThumbnailUrl;
    }

    public Subtitle call() throws IOException, SubtitleDecoderException {

        byte[] bytes = Utils.executeGet(vttThumbnailUrl, null);
        PKThumbnailsWebVttDecoder pkThumbnailsWebVttDecoder = new PKThumbnailsWebVttDecoder();
        return pkThumbnailsWebVttDecoder.decode(bytes, bytes.length, true);
    }
}