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

package com.kaltura.playkit.api.phoenix.model;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;

/**
 */

public class KalturaMediaAsset extends BaseResult {

    private int id;
    private int type;

    private long endDate;
    private long startDate;
    private Long externalIds;

    private String name;
    private JsonObject tags;
    private JsonObject metas;
    private String objectType;
    private String description;

    private List<KalturaThumbnail> images;
    private List<KalturaMediaFile> mediaFiles;


    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public long getEndDate() {
        return endDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public Long getExternalIds() {
        return externalIds;
    }

    public String getName() {
        return name;
    }

    public JsonObject getTags() {
        return tags;
    }

    public JsonObject getMetas() {
        return metas;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getDescription() {
        return description;
    }

    public List<KalturaThumbnail> getImages() {
        return images;
    }

    public List<KalturaMediaFile> getMediaFiles() {
        return mediaFiles;
    }
}