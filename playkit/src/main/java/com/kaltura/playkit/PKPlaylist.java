package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class PKPlaylist implements Parcelable {

    private String ks;
    private String id;
    private String name;
    private String description;
    private String thumbnailUrl;
    List<PKPlaylistMedia> mediaList;

    public PKPlaylist() {
    }

    public String getKs() {
        return ks;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public List<PKPlaylistMedia> getMediaList() {
        return mediaList;
    }

    public int getMediaListSize() {
        return mediaList != null ? mediaList.size() : 0;
    }

    public PKPlaylist setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PKPlaylist setId(String id) {
        this.id = id;
        return this;
    }

    public PKPlaylist setName(String name) {
        this.name = name;
        return this;
    }

    public PKPlaylist setDescription(String description) {
        this.description = description;
        return this;
    }

    public PKPlaylist setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public PKPlaylist setMediaList(List<PKPlaylistMedia> mediaList) {
        this.mediaList = mediaList;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.ks);
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeString(this.thumbnailUrl);
        dest.writeTypedList(this.mediaList);
    }

    protected PKPlaylist(Parcel in) {
        this.ks = in.readString();
        this.id = in.readString();
        this.name = in.readString();
        this.description = in.readString();
        this.thumbnailUrl = in.readString();
        int tmpPlaylistType = in.readInt();
        this.mediaList = in.createTypedArrayList(PKPlaylistMedia.CREATOR);
    }

    public static final Creator<PKPlaylist> CREATOR = new Creator<PKPlaylist>() {
        @Override
        public PKPlaylist createFromParcel(Parcel source) {
            return new PKPlaylist(source);
        }

        @Override
        public PKPlaylist[] newArray(int size) {
            return new PKPlaylist[size];
        }
    };
}
