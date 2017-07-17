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

package com.kaltura.playkit.api.base.model;

import android.text.TextUtils;

import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.playkit.api.ovp.OvpConfigs;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class BasePlaybackSource extends BaseResult {

    protected String format;
    protected String url;
    protected String protocols; // currently list of KalturaString objects
    protected List<KalturaDrmPlaybackPluginData> drm;


    public List<KalturaDrmPlaybackPluginData> getDrmData() {
        return drm;
    }

    /**
     * check if protocol is supported by this source.
     * Player can't redirect cross protocols so we make sure that the base protocol is supported
     * (included) by the source.
     *
     * @param protocol - the desired protocol for the source (base play url protocol)
     * @return true, if protocol is in the protocols list
     */
    public String getProtocol(String protocol) {
        if (protocols != null && protocols.length() > 0) {
            String protocolsLst[] = protocols.split(",");
            for (String prc : protocolsLst) {
                if (prc.equals(protocol)) {
                    return protocol;
                }
            }
        } else if (protocol.equals(OvpConfigs.DefaultHttpProtocol)) {
            return protocol;
        }

        return null;
    }

    public String getFormat() {
        return format;
    }

    public String getUrl() {
        return url;
    }

    public boolean hasDrmData() {
        return drm != null && drm.size() > 0;
    }

    public String getDrmSchemes(){
        StringBuilder schemes = new StringBuilder();
        if(hasDrmData()){
            for (KalturaDrmPlaybackPluginData drmPlaybackPluginData : drm){
                if(!TextUtils.isEmpty(drmPlaybackPluginData.getScheme())) {
                    schemes.append(drmPlaybackPluginData.getScheme()).append(",");
                }
            }
        }
        if(schemes.length() > 0){
            schemes.deleteCharAt(schemes.length()-1);
        }

        return schemes.toString();
    }


}
