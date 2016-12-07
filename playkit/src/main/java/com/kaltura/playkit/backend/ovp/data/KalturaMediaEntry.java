package com.kaltura.playkit.backend.ovp.data;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 31/10/2016.
 */

public class KalturaMediaEntry {

    private int mediaType;
    private String dataUrl;
    private String flavorParamsIds; //consider set as list
    private String id;
    private String name;
    private int msDuration;
    //private int licenseType = -1; //UNKNOWN

    public int getMediaType() {
        return mediaType;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public List<String> getFlavorParamsIdsList(){
        return Arrays.asList(flavorParamsIds.split(","));
    }

    public String getFlavorParamsIds() {
        return flavorParamsIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMsDuration() {
        return msDuration;
    }


}
