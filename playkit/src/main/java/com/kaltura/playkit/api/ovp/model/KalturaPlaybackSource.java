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

package com.kaltura.playkit.api.ovp.model;

import com.kaltura.playkit.api.base.model.BasePlaybackSource;
import com.kaltura.playkit.api.ovp.OvpConfigs;

import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */

public class KalturaPlaybackSource extends BasePlaybackSource {

    private int deliveryProfileId;
    private String flavorIds; //not clear if should be list<String> or string, currently list of KalturaString objects

    public int getDeliveryProfileId() {
        return deliveryProfileId;
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

    public String getFlavorIds() {
        return flavorIds;
    }

    public List<String> getFlavorIdsList() {
        return Arrays.asList(flavorIds.split(","));
    }

    public boolean hasFlavorIds() {
        return flavorIds != null && flavorIds.length() > 0;
    }

}
