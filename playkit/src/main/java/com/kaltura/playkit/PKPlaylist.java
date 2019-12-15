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
    private PKPlaylistType playlistType;
    private Integer duration;
    List<PKPlaylistMedia> playlistMediaList;


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

    public PKPlaylistType getPlaylistType() {
        return playlistType;
    }

    public Integer getDuration() {
        return duration;
    }

    public List<PKPlaylistMedia> getPlaylistMediaList() {
        return playlistMediaList;
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

    public PKPlaylist setPlaylistType(PKPlaylistType playlistType) {
        this.playlistType = playlistType;
        return this;
    }

    public PKPlaylist setDuration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public PKPlaylist setPlaylistMediaList(List<PKPlaylistMedia> playlistMediaList) {
        this.playlistMediaList = playlistMediaList;
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
        dest.writeInt(this.playlistType == null ? -1 : this.playlistType.ordinal());
        dest.writeValue(this.duration);
        dest.writeTypedList(this.playlistMediaList);
    }

    protected PKPlaylist(Parcel in) {
        this.ks = in.readString();
        this.id = in.readString();
        this.name = in.readString();
        this.description = in.readString();
        this.thumbnailUrl = in.readString();
        int tmpPlaylistType = in.readInt();
        this.playlistType = tmpPlaylistType == -1 ? null : PKPlaylistType.values()[tmpPlaylistType];
        this.duration = (Integer) in.readValue(Integer.class.getClassLoader());
        this.playlistMediaList = in.createTypedArrayList(PKPlaylistMedia.CREATOR);
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
