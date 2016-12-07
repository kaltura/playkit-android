package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public class KalturaLoginResponse extends BaseResult {

    KalturaLoginSession loginSession;
    //User object in response is currently irrelevant


    public KalturaLoginSession getLoginSession() {
        return loginSession;
    }
}
