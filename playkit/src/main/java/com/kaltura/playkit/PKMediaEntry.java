package com.kaltura.playkit;

import java.util.List;

public class PKMediaEntry {
    private String id;
    private List<PKMediaSource> sources;
    private long duration; //in milliseconds
    private MediaEntryType mediaType;

    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public PKMediaEntry setSources(List<PKMediaSource> sources) {
        this.sources = sources;
        return this;
    }

    public List<PKMediaSource> getSources() {
        return sources;
    }

    public PKMediaEntry setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public PKMediaEntry setMediaType(MediaEntryType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public MediaEntryType getMediaType() {
        return mediaType;
    }


    public enum MediaEntryType {
        Vod,
        Live,
        Unknown
    }
}
