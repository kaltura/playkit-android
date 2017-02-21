package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

/**
 * Created by tehilarozin on 28/11/2016.
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
