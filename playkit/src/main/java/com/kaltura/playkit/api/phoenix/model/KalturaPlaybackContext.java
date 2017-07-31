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

package com.kaltura.playkit.api.phoenix.model;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.api.base.model.BasePlaybackContext;
import com.kaltura.playkit.api.phoenix.PhoenixErrorHelper;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlaybackContext extends BasePlaybackContext{

    private ArrayList<KalturaPlaybackSource> sources;


    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }


    @Override
    protected ErrorElement getErrorElement(KalturaAccessControlMessage message) {
        switch (message.getCode()){
            case "OK":
                return null;
            default:
                return PhoenixErrorHelper.getErrorElement(message.getCode(), message.getMessage());
        }
    }
}
