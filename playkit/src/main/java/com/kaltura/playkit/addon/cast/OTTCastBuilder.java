package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public class OTTCastBuilder extends BasicCastBuilder<OTTCastBuilder> {


    public OTTCastBuilder setFormat(@NonNull String format) {
        mCastInfo.setFormat(format);
        return this;
    }


    public OTTCastBuilder setInitObject(@NonNull String initObject) {
        mCastInfo.setInitObject(initObject);
        return this;
    }


    @Override
    protected CastConfigHelper getCastHelper() {

        return new OTTCastConfigHelper();

    }


    @Override
    protected void validate(CastInfo castInfo) throws IllegalArgumentException {

        super.validate(castInfo);

        if (TextUtils.isEmpty(castInfo.getFormat())) {
            throw new IllegalArgumentException();
        }

        if (TextUtils.isEmpty(castInfo.getInitObject())) {
            throw new IllegalArgumentException();
        }
    }


}
