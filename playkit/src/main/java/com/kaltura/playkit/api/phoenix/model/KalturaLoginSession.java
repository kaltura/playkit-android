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

/**
 * @hide
 */

public class KalturaLoginSession extends BaseResult {
    String refreshToken;
    String ks;

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getKs() {
        return ks;
    }
}
