package com.kaltura.playkit;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PKMediaEntry implements Parcelable{
    private String id;
    private List<PKMediaSource> sources;
    private long duration; //in milliseconds
    private MediaEntryType mediaType;
    private Map<String,String> metadata;

    public PKMediaEntry(){}

    public PKMediaEntry(Parcel in) {
        id = in.readString();
        duration = in.readLong();
        mediaType = Utils.byValue(MediaEntryType.class, in.readString());// MediaEntryType.valueOf(in.readString());
        metadata = Utils.bundleToMap(in.readBundle(), String.class);// in.readBundle(String.class.getClassLoader());
        sources = in.createTypedArrayList(PKMediaSource.CREATOR);
    }

    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public PKMediaEntry setMetadata(Map<String,String> metadata){
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

    public boolean hasSources(){
        return sources != null && sources.size() > 0;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(duration);
        dest.writeString(mediaType.name());
        if(metadata != null) {
            dest.writeBundle(Utils.mapToBundle(metadata));
        } else {
            dest.writeBundle(new Bundle());
        }
        if(sources != null){
            dest.writeTypedList(sources);
        } else {
            dest.writeTypedList(Collections.EMPTY_LIST);
        }
    }

    public static final Creator<PKMediaEntry> CREATOR = new Creator<PKMediaEntry>() {
        @Override
        public PKMediaEntry createFromParcel(Parcel in) {
            return new PKMediaEntry(in);
        }

        @Override
        public PKMediaEntry[] newArray(int size) {
            return new PKMediaEntry[size];
        }
    };

    public enum MediaEntryType {
        Vod,
        Live,
        Unknown
    }
}
