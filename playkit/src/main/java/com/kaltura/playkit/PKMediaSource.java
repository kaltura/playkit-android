package com.kaltura.playkit;

import java.util.List;

public class PKMediaSource {
    private String id;
    private String url;
    private PKMediaFormat mediaFormat;
    private List<PKDrmParams> drmData;

    public String getId() {
        return id;
    }

    public PKMediaSource setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PKMediaSource setUrl(String url) {
        this.url = url;
        return this;
    }

    public List<PKDrmParams> getDrmData() {
        return drmData;
    }

    public PKMediaSource setDrmData(List<PKDrmParams> drmData) {
        this.drmData = drmData;
        return this;
    }

    public PKMediaFormat getMediaFormat() {
        return mediaFormat;
    }

    public PKMediaSource setMediaFormat(PKMediaFormat mediaFormat) {
        if (mediaFormat != null) {
            this.mediaFormat = mediaFormat;
        } else if (url != null) {
            this.mediaFormat = PKMediaFormat.getMediaFormat(url);
        } else {
            this.mediaFormat = mediaFormat;
        }
        return this;
    }
}
