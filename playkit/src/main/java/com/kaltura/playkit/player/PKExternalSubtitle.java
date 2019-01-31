package com.kaltura.playkit.player;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.Format;
import com.kaltura.playkit.PKSubtitleFormat;
import com.kaltura.playkit.utils.Consts;

public class PKExternalSubtitle implements Parcelable {

    private String url;
    private String id;
    private String mimeType;
    private int selectionFlags = Consts.TRACK_UNSELECTED_FLAG;
    private String language;
    private String label;
    private String containerMimeType = null;
    private String codecs = null;
    private int bitrate = Format.NO_VALUE;
    private boolean isDefault;

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

    public boolean isDefault() {
        return isDefault;
    }

    public PKExternalSubtitle setMimeType(PKSubtitleFormat subtitleFormat) {
        if (subtitleFormat != null) {
            this.mimeType = subtitleFormat.mimeType;
        }
        return this;
    }

    public PKExternalSubtitle setUrl(@NonNull String url) {
        this.url = url;
        return this;
    }

    public PKExternalSubtitle setLanguage(@NonNull String language) {
        this.language = language;
        return this;
    }

    public PKExternalSubtitle setLabel(@NonNull String label) {
        this.label = label;
        return this;
    }

    public PKExternalSubtitle setDefault() {
        this.isDefault = true;
        setSelectionFlags(Consts.DEFAULT_TRACK_SELECTION_FLAG);
        return this;
    }

    private void setSelectionFlags(int selectionFlags) {
        this.selectionFlags = selectionFlags;
    }

    protected PKExternalSubtitle(Parcel in) {
        url = in.readString();
        id = in.readString();
        selectionFlags = in.readInt();
        language = in.readString();
        label = in.readString();
        containerMimeType = in.readString();
        codecs = in.readString();
        bitrate = in.readInt();
        isDefault = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(id);
        dest.writeInt(selectionFlags);
        dest.writeString(language);
        dest.writeString(label);
        dest.writeString(containerMimeType);
        dest.writeString(codecs);
        dest.writeInt(bitrate);
        dest.writeByte((byte) (isDefault ? 1 : 0));
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
