package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.connect.ErrorElement;

/**
 * @hide
 */

public class KalturaLoginResponse extends BaseResult {

    private KalturaLoginSession loginSession;
    private KalturaOTTUser user;

    public KalturaLoginResponse(ErrorElement error) {
        super(error);
    }

    public KalturaLoginSession getLoginSession() {
        return loginSession;
    }

    public KalturaOTTUser getUser() {
        return user;
    }
}
