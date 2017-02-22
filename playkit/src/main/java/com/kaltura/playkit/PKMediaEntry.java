package com.kaltura.playkit;

import java.util.List;
import java.util.Map;

public class PKMediaEntry {
    private String id;
    private List<PKMediaSource> sources;
    private long duration; //in milliseconds
    private MediaEntryType mediaType;
    private Map<String, String> metadata;

    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public PKMediaEntry setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKMediaEntry that = (PKMediaEntry) o;

        if (duration != that.duration) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (sources != null ? !sources.equals(that.sources) : that.sources != null) return false;
        for (int i = 0; i < sources.size(); i++) {
            if (!sources.get(i).equals(that.sources.get(i))) {
                return false;
            }
        }
        if (mediaType != that.mediaType) return false;
        return metadata != null ? metadata.equals(that.metadata) : that.metadata == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (sources != null ? sources.hashCode() : 0);
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }
}
