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

    @Override
    public String getMemberValue(String name){
        switch (name){
            case "id":
                return id;
            case "flavorParamsId":
                return flavorParamsId;
            case "bitrate":
                return bitrate+"";
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
}
