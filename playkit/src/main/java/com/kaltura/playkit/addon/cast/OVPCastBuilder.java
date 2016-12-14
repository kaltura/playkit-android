package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public class OVPCastBuilder extends KCastBuilder<OVPCastBuilder> {


    public OVPCastBuilder setKs(@NonNull String ks) {
        mKCastInfo.setKs(ks);
        return this;
    }


    @Override
    protected CastConfigHelper getCastHelper() {

        return new OVPCastConfigHelper();

    }


    @Override
    protected void validate(KCastInfo kCastInfo) throws IllegalArgumentException {

        super.validate(kCastInfo);

        // ks isn't mandatory in OVP environment, but if you do set a ks it must be valid
        String mwEmbedUrl = kCastInfo.getMwEmbedUrl();
        if (mwEmbedUrl != null && TextUtils.isEmpty(mwEmbedUrl)) {
            throw new IllegalArgumentException();
        }


    }


}
