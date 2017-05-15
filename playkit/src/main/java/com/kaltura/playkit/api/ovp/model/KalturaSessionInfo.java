package com.kaltura.playkit.api.ovp.model;

import com.kaltura.netkit.connect.response.BaseResult;

/**
 * @hide
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
