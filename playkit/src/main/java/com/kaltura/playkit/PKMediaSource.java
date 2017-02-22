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
        if (mediaFormat == null && url != null) {
            this.mediaFormat = PKMediaFormat.valueOfUrl(url);
        }
        return mediaFormat;
    }

    public PKMediaSource setMediaFormat(PKMediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
        return this;
    }

    public boolean hasDrmParams() {
        return (drmData != null && drmData.size() > 0) ? true : false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKMediaSource that = (PKMediaSource) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (mediaFormat != that.mediaFormat) return false;
        if (drmData != null ? drmData.equals(that.drmData) : that.drmData == null) ;
        for(int i = 0; i < drmData.size(); i++){
            if(!drmData.get(i).equals(that.drmData.get(i))){
                return false;
            }
        }
        return true;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (mediaFormat != null ? mediaFormat.hashCode() : 0);
        result = 31 * result + (drmData != null ? drmData.hashCode() : 0);
        return result;
    }
}
