/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public class OVPCastBuilder extends BasicCastBuilder<OVPCastBuilder> {


    public OVPCastBuilder setKs(@NonNull String ks) {
        castInfo.setKs(ks);
        return this;
    }


    @Override
    protected CastConfigHelper getCastHelper() {

        return new OVPCastConfigHelper();

    }


    @Override
    protected void validate(CastInfo castInfo) throws IllegalArgumentException {

        super.validate(castInfo);

        // ks isn't mandatory in OVP environment, but if you do set a ks it must be valid
        String ks = castInfo.getKs();
        if (ks != null && TextUtils.isEmpty(ks)) {
            throw new IllegalArgumentException();
        }


    }


}
