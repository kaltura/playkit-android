package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class PKPlaylistMedia implements Parcelable {

    private String id;
    private String ks;
    private String name;
    private String tags;
    private String dataUrl;
    private String description;
    private String thumbnailUrl;
    private String flavorParamsIds;
    private PKMediaEntry.MediaEntryType type;
    private long msDuration;
    public Map<String, String> metadata;

    public PKPlaylistMedia() { }

    public String getId() {
        return id;
    }

    public String getKs() { return ks; }

    public String getName() {
        return name;
    }

    public String getTags() {
        return tags;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getFlavorParamsIds() {
        return flavorParamsIds;
    }

    public PKMediaEntry.MediaEntryType getType() {
        return type;
    }

    public Map<String,String> getMetadata() {
        return metadata;
    }

    public long getMsDuration() { return msDuration; }

    public PKPlaylistMedia setId(String id) {
        this.id = id;
        return this;
    }

    public PKPlaylistMedia setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PKPlaylistMedia setName(String name) {
        this.name = name;
        return this;
    }

    public PKPlaylistMedia setTags(String tags) {
        this.tags = tags;
        return this;
    }

    public PKPlaylistMedia setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
        return this;
    }

    public PKPlaylistMedia setDescription(String description) {
        this.description = description;
        return this;
    }

    public PKPlaylistMedia setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public PKPlaylistMedia setFlavorParamsIds(String flavorParamsIds) {
        this.flavorParamsIds = flavorParamsIds;
        return this;
    }

    public PKPlaylistMedia setType(PKMediaEntry.MediaEntryType type) {
        this.type = type;
        return this;
    }

    public PKPlaylistMedia setMsDuration(long msDuration) {
        this.msDuration = msDuration;
        return this;
    }

    public PKPlaylistMedia setMetadata(Map<String,String> metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.ks);
        dest.writeString(this.name);
        dest.writeString(this.tags);
        dest.writeString(this.dataUrl);
        dest.writeString(this.description);
        dest.writeString(this.thumbnailUrl);
        dest.writeString(this.flavorParamsIds);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeLong(this.msDuration);
        dest.writeInt(this.metadata.size());
        for (Map.Entry<String, String> entry : this.metadata.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    protected PKPlaylistMedia(Parcel in) {
        this.id = in.readString();
        this.ks = in.readString();
        this.name = in.readString();
        this.tags = in.readString();
        this.dataUrl = in.readString();
        this.description = in.readString();
        this.thumbnailUrl = in.readString();
        this.flavorParamsIds = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : PKMediaEntry.MediaEntryType.values()[tmpType];
        this.msDuration = in.readLong();
        int metadataSize = in.readInt();
        this.metadata = new HashMap<>(metadataSize);
        for (int i = 0; i < metadataSize; i++) {
            String key = in.readString();
            String value = in.readString();
            this.metadata.put(key, value);
        }
    }

    public static final Creator<PKPlaylistMedia> CREATOR = new Creator<PKPlaylistMedia>() {
        @Override
        public PKPlaylistMedia createFromParcel(Parcel source) {
            return new PKPlaylistMedia(source);
        }

        @Override
        public PKPlaylistMedia[] newArray(int size) {
            return new PKPlaylistMedia[size];
        }
    };
}
