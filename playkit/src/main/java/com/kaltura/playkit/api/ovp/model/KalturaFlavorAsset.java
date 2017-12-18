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

package com.kaltura.playkit.api.ovp.model;

/**
 * @hide
 */

public class KalturaFlavorAsset implements FlavorAssetsFilter.Filterable {

    private String id;
    private String flavorParamsId;
    private String fileExt;
    private int bitrate;
    private int width;
    private int height;
    private String videoCodecId;

    public enum VideoCodecType {
        H264,
        H265,
        UNKNOWN
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFlavorParamsId() {
        return flavorParamsId;
    }

    public String getFileExt() {
        return fileExt;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getVideoCodecId() {
        return videoCodecId;
    }

    //TODO remove all setters after we remove the MOCK logic.
    public void setFlavorParamsId(String flavorParamsId) {
        this.flavorParamsId = flavorParamsId;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setVideoCodecId(String videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    @Override
    public String getMemberValue(String name) {
        switch (name) {
            case "id":
                return id;
            case "flavorParamsId":
                return flavorParamsId;
            case "bitrate":
                return bitrate + "";
        }
        return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KalturaFlavorAsset that = (KalturaFlavorAsset) o;
        return id != null && that.id != null && id.equals(that.id);

        /*if (!id.equals(that.id)) return false;
        if (bitrate != that.bitrate) return false;
        return fileExt != null ? fileExt.equals(that.fileExt) : that.fileExt == null;*/
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (fileExt != null ? fileExt.hashCode() : 0);
        result = 31 * result + bitrate;
        return result;
    }

    public boolean isH265() {
        return videoCodecId.equals("hvc1") || videoCodecId.equals("hev1");
    }

    public boolean isH264() {
        return videoCodecId.equals("avc1");
    }

    public VideoCodecType getVideoCodecType() {
        if (videoCodecId.equals("hvc1") || videoCodecId.equals("hev1")) {
            return VideoCodecType.H265;
        } else if (videoCodecId.equals("avc1")) {
            return VideoCodecType.H264;
        }

        return VideoCodecType.UNKNOWN;
    }
}
