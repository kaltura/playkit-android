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
    int id;
    int type;
    String objectType;
    String name;
    String description;
    JsonObject metas;
    JsonObject tags;
    Long externalIds;
    long startDate;
    long endDate;
    List<KalturaThumbnail> images;
    List<KalturaMediaFile> mediaFiles;

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject getMetas() {
        return metas;
    }

    public JsonObject getTags() {
        return tags;
    }

    public Long getExternalIds() {
        return externalIds;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public List<KalturaThumbnail> getImages() {
        return images;
    }

    public List<KalturaMediaFile> getFiles() {
        return mediaFiles;
    }

}
