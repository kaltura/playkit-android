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

    private int msDuration;
    private Integer dvrStatus; // 1-LIVE DVR  0-LIVE

    private String id;
    private String name;
    private String tags;
    private String dataUrl;
    private String description;
    private String thumbnailUrl;
    private String flavorParamsIds;

    /** indicate the media type: {@link KalturaEntryType} **/
    private KalturaEntryType type;

    public int getMsDuration() {
        return msDuration;
    }

    public Integer getDvrStatus() {
        return dvrStatus;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTags() {
        return tags;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getFlavorParamsIds() {
        return flavorParamsIds;
    }

    public KalturaEntryType getType() {
        return type;
    }

    public List<String> getFlavorParamsIdsList(){
        return Arrays.asList(flavorParamsIds.split(","));
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasId() {
        return id != null;
    }
    public boolean hasName() {
        return name != null;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasThumbnail() {
        return thumbnailUrl != null;
    }

    public boolean hasDvrStatus() {
        return dvrStatus != null;
    }

    public boolean hasTags() {
        return tags != null;
    }
}