package com.connect.backend.ovp.data;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 31/10/2016.
 */

public class KalturaMediaEntry {

    private String id;
    private String name;

    /** indicate the media type: {@link KalturaEntryType} **/
    private KalturaEntryType type;

    private String dataUrl;
    private String flavorParamsIds;
    private int msDuration;

    public KalturaEntryType getType() {
        return type;
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
