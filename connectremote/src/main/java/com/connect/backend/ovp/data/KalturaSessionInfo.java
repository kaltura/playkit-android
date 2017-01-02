package com.connect.backend.ovp.data;

import com.connect.backend.BaseResult;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public class KalturaSessionInfo extends BaseResult {

    String sessionType;
    long expiry;
    String userId;


    public long getExpiry() {
        return expiry;
    }

    public String getUserId() {
        return userId;
    }
}
