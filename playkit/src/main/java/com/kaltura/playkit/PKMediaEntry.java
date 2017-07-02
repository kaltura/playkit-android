/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PKMediaEntry implements Parcelable {
    
    private String id;
    private String name;
    private List<PKMediaSource> sources;
    private long duration; //in milliseconds
    private MediaEntryType mediaType;
    private Map<String,String> metadata;

    public PKMediaEntry(){}


    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public PKMediaEntry setName(String name) {
        this.name = name;
        return this;
    }

    public PKMediaEntry setMetadata(Map<String,String> metadata){
        this.metadata = metadata;
        return this;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public enum MediaEntryType {
        Vod,
        Live,
        Unknown
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        if (sources != null) {
            dest.writeTypedList(this.sources);
        } else {
            dest.writeTypedList(Collections.EMPTY_LIST);
        }
        dest.writeLong(this.duration);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        if (this.metadata != null) {
            dest.writeInt(this.metadata.size());
            for (Map.Entry<String, String> entry : this.metadata.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(-1);
        }
    }

    protected PKMediaEntry(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.sources = in.createTypedArrayList(PKMediaSource.CREATOR);
        this.duration = in.readLong();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MediaEntryType.values()[tmpMediaType];
        int metadataSize = in.readInt();
        if (metadataSize == -1) {
            this.metadata = null;
        } else {
            this.metadata = new HashMap<String, String>(metadataSize);
            for (int i = 0; i < metadataSize; i++) {
                String key = in.readString();
                String value = in.readString();
                this.metadata.put(key, value);
            }
        }
    }

    public static final Creator<PKMediaEntry> CREATOR = new Creator<PKMediaEntry>() {
        @Override
        public PKMediaEntry createFromParcel(Parcel source) {
            return new PKMediaEntry(source);
        }

        @Override
        public PKMediaEntry[] newArray(int size) {
            return new PKMediaEntry[size];
        }
    };
}
