package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

/**
 * @hide
 */

public class KalturaLoginResponse extends BaseResult {

    KalturaLoginSession loginSession;
    //User object in response is currently irrelevant


    public KalturaLoginSession getLoginSession() {
        return loginSession;
    }
}
