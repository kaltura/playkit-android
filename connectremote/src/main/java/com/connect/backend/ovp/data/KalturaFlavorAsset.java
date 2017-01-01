package com.connect.backend.ovp.data;

/**
 * Created by tehilarozin on 31/10/2016.
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
