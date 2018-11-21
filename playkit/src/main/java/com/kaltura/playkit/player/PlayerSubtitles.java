package com.kaltura.playkit.player;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.exoplayer2.util.MimeTypes;

public class PlayerSubtitles implements Parcelable {

    public enum SubtitleMimeType {
        TEXT_VTT, APPLICATION_SUBRIP
    }

    private String id;
    private String mimeType;
    private String url;
    private int selectionFlags = 0;
    private String language;

    public PlayerSubtitles() {

    }

    public String getId() {
        return id;
    }

    public PlayerSubtitles setId(String id) {
        this.id = id;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PlayerSubtitles setMimeType(SubtitleMimeType mimeType) {
        switch (mimeType) {
            case TEXT_VTT:
                this.mimeType = MimeTypes.TEXT_VTT;
                break;
            case APPLICATION_SUBRIP:
                this.mimeType = MimeTypes.APPLICATION_SUBRIP;
                break;
            default:
                break;
        }
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PlayerSubtitles setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getSelectionFlags() {
        return selectionFlags;
    }

    public String getLanguage() {
        return language;
    }

    public PlayerSubtitles setLanguage(String language) {
        this.language = language;
        return this;
    }

    protected PlayerSubtitles(Parcel in) {
        id = in.readString();
        mimeType = in.readString();
        url = in.readString();
        selectionFlags = in.readInt();
        language = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(mimeType);
        dest.writeString(url);
        dest.writeInt(selectionFlags);
        dest.writeString(language);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayerSubtitles> CREATOR = new Creator<PlayerSubtitles>() {
        @Override
        public PlayerSubtitles createFromParcel(Parcel in) {
            return new PlayerSubtitles(in);
        }

        @Override
        public PlayerSubtitles[] newArray(int size) {
            return new PlayerSubtitles[size];
        }
    };
}
