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
import java.util.List;

public class PKMediaSource implements Parcelable {
    private String id;
    private String url;
    private PKMediaFormat mediaFormat;
    private List<PKDrmParams> drmData;

    public PKMediaSource(){}

    protected PKMediaSource(Parcel in) {
        id = in.readString();
        url = in.readString();
        mediaFormat = Utils.byValue(PKMediaFormat.class, in.readString());
        drmData = in.createTypedArrayList(PKDrmParams.CREATOR);
    }

    public String getId() {
        return id;
    }

    public PKMediaSource setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PKMediaSource setUrl(String url) {
        this.url = url;
        return this;
    }

    public List<PKDrmParams> getDrmData() {
        return drmData;
    }

    public PKMediaSource setDrmData(List<PKDrmParams> drmData) {
        this.drmData = drmData;
        return this;
    }

    public PKMediaFormat getMediaFormat() {
        if (mediaFormat == null && url != null) {
            this.mediaFormat = PKMediaFormat.valueOfUrl(url);
        }
        return mediaFormat;
    }

    public PKMediaSource setMediaFormat(PKMediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
        return this;
    }


    public boolean hasDrmParams() {
        return (drmData != null && drmData.size() > 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(url);
        dest.writeString(mediaFormat.name());
        if(drmData != null) {
            dest.writeTypedList(drmData);
        } else {
            dest.writeTypedList(Collections.EMPTY_LIST);
        }
    }

    public static final Creator<PKMediaSource> CREATOR = new Creator<PKMediaSource>() {
        @Override
        public PKMediaSource createFromParcel(Parcel in) {
            return new PKMediaSource(in);
        }

        @Override
        public PKMediaSource[] newArray(int size) {
            return new PKMediaSource[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKMediaSource that = (PKMediaSource) o;

        if (!id.equals(that.id)) return false;
        return url.equals(that.url);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }
}
