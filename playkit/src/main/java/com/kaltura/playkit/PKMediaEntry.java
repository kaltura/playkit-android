package com.kaltura.playkit;

import java.util.List;

public class PKMediaEntry {
    private String id;
    private List<PKMediaSource> sources;
    private long duration;

    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public PKMediaEntry setSources(List<PKMediaSource> sources) {
        this.sources = sources;
        return this;
    }

    public PKMediaEntry setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public String getId() {
        return id;
    }

    public List<PKMediaSource> getSources() {
        return sources;
    }

    public long getDuration() {
        return duration;
    }
}
