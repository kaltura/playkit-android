package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

/**
 * @hide
 */

public class KalturaLoginResponse extends BaseResult {

    private KalturaLoginSession loginSession;
    private KalturaOTTUser user;


    public KalturaLoginSession getLoginSession() {
        return loginSession;
    }

    public KalturaOTTUser getUser() {
        return user;
    }
}
