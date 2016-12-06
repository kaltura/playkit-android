package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public class KalturaSession extends BaseResult {

    String sessionType;
    long expiry;
    String userId;
    String ks;

    public long getExpiry() {
        return expiry;
    }

    public String getKs() {
        return ks;
    }
}
