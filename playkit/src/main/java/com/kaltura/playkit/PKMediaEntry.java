package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PKMediaEntry {
    private String id;
    private List<PKMediaSource> sources;
    private long duration;
    private MediaEntryType mediaType;
    private Map<String, Object> metadata;

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

    public PKMediaEntry setMetadata(String key, Object data){
        if(metadata == null){
            metadata = new HashMap<>();
        }
        metadata.put(key, data);
        return this;
    }

    public PKMediaEntry setMetadata(Map<String, Object> metas){
        if(metadata == null){
            metadata = new HashMap<>();
        }
        metadata.putAll(metas);
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

    @Nullable
    public Object getMetaData(@NonNull String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public enum MediaEntryType {
        VOD,
        LIVE
    }

}
