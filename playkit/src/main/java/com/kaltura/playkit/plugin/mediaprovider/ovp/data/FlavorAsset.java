package com.kaltura.playkit.plugin.mediaprovider.ovp.data;

/**
 * Created by tehilarozin on 31/10/2016.
 */

public class FlavorAsset /*implements Comparator*/ {

    private String id;
    private int flavorParamsId;
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

    public int getFlavorParamsId() {
        return flavorParamsId;
    }

    public void setFlavorParamsId(int flavorParamsId) {
        this.flavorParamsId = flavorParamsId;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlavorAsset that = (FlavorAsset) o;
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
