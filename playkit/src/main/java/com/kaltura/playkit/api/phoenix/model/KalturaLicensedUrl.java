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

import android.text.TextUtils;

import com.kaltura.netkit.connect.response.BaseResult;

/**
 * @hide
 */

public class KalturaLicensedUrl extends BaseResult {
    String mainUrl = "";
    String altUrl = "";

    public String getMainUrl() {
        return mainUrl;
    }

    public String getLicensedUrl() {
        return TextUtils.isEmpty(mainUrl) ? altUrl : mainUrl;
    }

    public String getAltUrl() {
        return altUrl;
    }
}
