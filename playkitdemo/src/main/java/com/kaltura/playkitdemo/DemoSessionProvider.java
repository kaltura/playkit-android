package com.kaltura.playkitdemo;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.PrimitiveResult;

/**
 * Created by tehilarozin on 16/11/2016.
 */

public class DemoSessionProvider implements SessionProvider {

    private String ks;

    private static DemoSessionProvider self;

    public static DemoSessionProvider getSessionProvider(){
        if(self == null){
            self = new DemoSessionProvider();
        }
        return self;
    }

    private DemoSessionProvider(){
    }

    public void setKs(String ks) {
        this.ks = ks;
    }

    @Override
    public String baseUrl() {
        return MockParams.PhoenixBaseUrl;
    }

    @Override
    public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
        if (completion!=null){
            completion.onComplete(new PrimitiveResult(this.ks));
        }
    }

    @Override
    public int partnerId() {
        return MockParams.OttPartnerId;
    }
}
