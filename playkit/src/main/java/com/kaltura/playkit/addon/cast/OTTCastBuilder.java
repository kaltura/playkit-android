package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public class OTTCastBuilder extends KCastBuilder<OTTCastBuilder> {


    public OTTCastBuilder setFormat(@NonNull String format) {
        mKCastInfo.setFormat(format);
        return this;
    }


    public OTTCastBuilder setInitObject(@NonNull String initObject) {
        mKCastInfo.setInitObject(initObject);
        return this;
    }


    @Override
    protected CastConfigHelper getCastHelper() {

        return new OTTCastConfigHelper();

    }


    @Override
    protected void validate(KCastInfo kCastInfo) throws IllegalArgumentException {

        super.validate(kCastInfo);

        if (TextUtils.isEmpty(kCastInfo.getFormat())) {
            throw new IllegalArgumentException();
        }

        if (TextUtils.isEmpty(kCastInfo.getInitObject())) {
            throw new IllegalArgumentException();
        }
    }


}
