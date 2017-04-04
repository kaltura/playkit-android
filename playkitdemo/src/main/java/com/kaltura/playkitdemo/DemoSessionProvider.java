package com.kaltura.playkitdemo;

import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;

/**
 * @hide
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
