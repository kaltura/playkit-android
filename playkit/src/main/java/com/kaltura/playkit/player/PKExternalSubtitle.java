package com.kaltura.playkit.player;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;

public class PKExternalSubtitle implements Parcelable {

    public enum SubtitleMimeType {
        TEXT_VTT, APPLICATION_SUBRIP
    }

    private String url;
    private String id;
    private String mimeType;
    private int selectionFlags = 0;
    private String language;
    private String label;
    private String containerMimeType = null;
    private String codecs = null;
    private int bitrate = Format.NO_VALUE;

    public PKExternalSubtitle() {
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getContainerMimeType() {
        return containerMimeType;
    }

    public String getCodecs() {
        return codecs;
    }

    public int getBitrate() {
        return bitrate;
    }

    public String getUrl() {
        return url;
    }

    public int getSelectionFlags() {
        return selectionFlags;
    }

    public String getLanguage() {
        return language;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PKExternalSubtitle setMimeType(SubtitleMimeType mimeType) {

        if(mimeType == null) {
            return this;
        }

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

    public PKExternalSubtitle setId(String id) {
        this.id = id;
        return this;
    }

    public PKExternalSubtitle setUrl(String url) {
        this.url = url;
        return this;
    }

    public PKExternalSubtitle setLanguage(String language) {
        this.language = language;
        return this;
    }

    public PKExternalSubtitle setLabel(String label) {
        this.label = label;
        return this;
    }

    protected PKExternalSubtitle(Parcel in) {
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

    public static final Creator<PKExternalSubtitle> CREATOR = new Creator<PKExternalSubtitle>() {
        @Override
        public PKExternalSubtitle createFromParcel(Parcel in) {
            return new PKExternalSubtitle(in);
        }

        @Override
        public PKExternalSubtitle[] newArray(int size) {
            return new PKExternalSubtitle[size];
        }
    };
}
