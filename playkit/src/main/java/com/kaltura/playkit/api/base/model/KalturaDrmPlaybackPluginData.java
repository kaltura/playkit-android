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

package com.kaltura.playkit.api.base.model;

import com.kaltura.netkit.connect.response.BaseResult;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class KalturaDrmPlaybackPluginData extends BaseResult {
    private String scheme;
    private String certificate;
    private String licenseURL;

    public String getLicenseURL() {
        return licenseURL;
    }

    public String getScheme() {
        return scheme;
    }
}
