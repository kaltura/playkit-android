package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public class TVPAPICastBuilder extends BasicCastBuilder<TVPAPICastBuilder> {


    public TVPAPICastBuilder setFormat(@NonNull String format) {
        castInfo.setFormat(format);
        return this;
    }


    public TVPAPICastBuilder setInitObject(@NonNull String initObject) {
        castInfo.setInitObject(initObject);
        return this;
    }


    @Override
    protected CastConfigHelper getCastHelper() {

        return new TVPAPICastConfigHelper();

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
