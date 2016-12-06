package com.kaltura.playkit;

import java.util.List;

public class PKMediaSource {
    private String id;
    private String url;
    //private String mimeType;
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

    /*public String getMimeType() {
        return mimeType;
    }

    public PKMediaSource setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }*/

    public List<PKDrmParams> getDrmData() {
        return drmData;
    }

    public PKMediaSource setDrmData(List<PKDrmParams> drmData) {
        this.drmData = drmData;
        return this;
    }
}
