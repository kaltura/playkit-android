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

import com.kaltura.playkit.api.base.model.BasePlaybackSource;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class KalturaPlaybackSource extends BasePlaybackSource {
    private int assetId;
    private int id;
    private String type; //Device types as defined in the system (MediaFileFormat)
    private long duration;
    private String externalId;

    public int getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }

    public String getType() {
        return type;
    }

}
