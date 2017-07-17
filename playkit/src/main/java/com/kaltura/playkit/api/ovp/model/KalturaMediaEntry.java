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

import java.util.Arrays;
import java.util.List;

/**
 * @hide
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

    public String getName() {
        return name;
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
