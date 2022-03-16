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
import android.text.TextUtils;

import com.kaltura.playkit.player.PKExternalSubtitle;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class PKMediaEntry implements Parcelable {

    private String id;
    private String name;
    private List<PKMediaSource> sources;
    private long duration; //in milliseconds
    private MediaEntryType mediaType;
    private boolean isVRMediaType;
    private Map<String, String> metadata;
    private List<PKExternalSubtitle> externalSubtitleList;
    private String externalVttThumbnailUrl;

    public PKMediaEntry() {
    }

    public PKMediaEntry setId(String id) {
        this.id = id;
        return this;
    }

    public PKMediaEntry setName(String name) {
        this.name = name;
        return this;
    }

    public PKMediaEntry setIsVRMediaType(boolean isVRMediaType) {
        this.isVRMediaType = isVRMediaType;
        return this;
    }

    public PKMediaEntry setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PKMediaEntry setExternalSubtitleList(List<PKExternalSubtitle> externalSubtitleList) {
        this.externalSubtitleList = externalSubtitleList;
        if (externalSubtitleList != null) {
            ListIterator<PKExternalSubtitle> externalSubtitleListIterator = externalSubtitleList.listIterator();

            while (externalSubtitleListIterator.hasNext()) {
                PKExternalSubtitle pkExternalSubtitle = externalSubtitleListIterator.next();
                PKSubtitleFormat urlFormat = PKSubtitleFormat.valueOfUrl(pkExternalSubtitle.getUrl());

                if (urlFormat != null && pkExternalSubtitle.getMimeType() == null) {
                    pkExternalSubtitle.setMimeType(urlFormat);
                }

                if (TextUtils.isEmpty(pkExternalSubtitle.getUrl()) || (urlFormat != null && !urlFormat.mimeType.equals(pkExternalSubtitle.getMimeType()))) {
                    externalSubtitleListIterator.remove();
                }
            }
        }

        return this;
    }

    public PKMediaEntry setExternalVttThumbnailUrl(String externalVttThumbnailUrl) {
        this.externalVttThumbnailUrl = externalVttThumbnailUrl;
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

    public boolean isVRMediaType() {
        return isVRMediaType;
    }

    public List<PKMediaSource> getSources() {
        return sources;
    }

    public boolean hasSources() {
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<PKExternalSubtitle> getExternalSubtitleList() {
        return externalSubtitleList;
    }

    public String getExternalVttThumbnailUrl() {
        return externalVttThumbnailUrl;
    }

    public enum MediaEntryType {
        Vod,
        Live,
        DvrLive,
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
            dest.writeTypedList(Collections.emptyList());
        }
        dest.writeLong(this.duration);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeByte(this.isVRMediaType ? (byte) 1 : (byte) 0);
        if (this.metadata != null) {
            dest.writeInt(this.metadata.size());
            for (Map.Entry<String, String> entry : this.metadata.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(-1);
        }
        dest.writeTypedList(this.externalSubtitleList);
        dest.writeString(this.externalVttThumbnailUrl);
    }

    protected PKMediaEntry(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.sources = in.createTypedArrayList(PKMediaSource.CREATOR);
        this.duration = in.readLong();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MediaEntryType.values()[tmpMediaType];
        this.isVRMediaType = in.readByte() != 0;
        int metadataSize = in.readInt();
        if (metadataSize == -1) {
            this.metadata = null;
        } else {
            this.metadata = new HashMap<>(metadataSize);
            for (int i = 0; i < metadataSize; i++) {
                String key = in.readString();
                String value = in.readString();
                if (!TextUtils.isEmpty(key)) {
                    this.metadata.put(key, value);
                }
            }
        }
        this.externalSubtitleList = in.createTypedArrayList(PKExternalSubtitle.CREATOR);
        this.externalVttThumbnailUrl = in.readString();
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
