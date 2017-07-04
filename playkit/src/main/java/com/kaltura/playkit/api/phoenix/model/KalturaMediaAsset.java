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

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;

/**
 */

public class KalturaMediaAsset extends BaseResult {
    int id;
    int type;
    String name;
    List<KalturaMediaFile> mediaFiles;

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public List<KalturaMediaFile> getFiles() {
        return mediaFiles;
    }

}
