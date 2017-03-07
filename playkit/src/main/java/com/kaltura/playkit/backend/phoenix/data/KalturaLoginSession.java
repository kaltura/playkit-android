package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

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
