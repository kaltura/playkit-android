package com.kaltura.playkit.backend.tvpapi;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.backend.base.BaseSessionProvider;
import com.kaltura.playkit.backend.PrimitiveResult;

/**
 * Created by tehilarozin on 11/12/2016.
 */

public class TvpapiSessionProvider extends BaseSessionProvider {

    private int partnerId;

    public TvpapiSessionProvider(String baseUrl, int partnerId) {
        super(baseUrl, "");
        this.partnerId = partnerId;
    }

    @Override
    public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
        // for now does nothing!
    }

    @Override
    public int partnerId() {
        return this.partnerId;
    }



    @Override
    protected String validateSession() {
        return null;
    }
}
