package com.connect.backend.phoenix.data;

import com.connect.backend.BaseResult;

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
