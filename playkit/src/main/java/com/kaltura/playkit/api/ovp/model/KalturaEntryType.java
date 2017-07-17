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

import com.google.gson.annotations.SerializedName;

/**
 * Created by tehilarozin on 21/12/2016.
 */

public enum KalturaEntryType {

    @SerializedName("-1")
    AUTOMATIC("-1"),
    @SerializedName("1")
    MEDIA_CLIP("1"),
    @SerializedName("7")
    LIVE_STREAM("7"),
    @SerializedName("5")
    PLAYLIST("5");

    public String type;

    KalturaEntryType(String type) {
        this.type = type;
    }
}
