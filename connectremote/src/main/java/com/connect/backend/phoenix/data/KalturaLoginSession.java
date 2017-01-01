package com.connect.backend.phoenix.data;

import com.connect.backend.BaseResult;

/**
 * Created by tehilarozin on 27/11/2016.
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
