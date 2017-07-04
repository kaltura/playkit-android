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

import android.text.TextUtils;

import com.kaltura.playkit.PKLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itanbarpeled on 14/12/2016.
 */


class OVPCastConfigHelper extends CastConfigHelper {


    @Override
    protected String getSessionInfo(CastInfo castInfo) {

        return castInfo.getKs();

    }



    @Override
    protected void setProxyData(JSONObject flashVars, String ks, String fileFormat,
                                String entryId) {


        JSONObject proxyData = new JSONObject();


        //it's possible to work in OVP environment without ks
        if (!TextUtils.isEmpty(ks)) {

            try {

                proxyData.put("ks", ks);
                flashVars.put("proxyData", proxyData);

            } catch(JSONException e) {
               log.e(e.getMessage());
            }

        }


    }


}
