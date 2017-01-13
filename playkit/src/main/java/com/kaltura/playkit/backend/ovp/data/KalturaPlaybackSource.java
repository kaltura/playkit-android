package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.ovp.OvpConfigs;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaPlaybackSource {

    private int deliveryProfileId;
    private String format;
    private String url;
    private String protocols; // currently list of KalturaString objects
    private String flavorIds; //not clear if should be list<String> or string, currently list of KalturaString objects
    private List<KalturaDrmEntryPlayingPluginData> drm;


    public int getDeliveryProfileId() {
        return deliveryProfileId;
    }

    public List<KalturaDrmEntryPlayingPluginData> getDrmData() {
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


    public String getFlavorIds() {
        return flavorIds;
    }

    public List<String> getFlavorIdsList() {
        return Arrays.asList(flavorIds.split(","));
    }

    public boolean hasFlavorIds() {
        return flavorIds != null && flavorIds.length() > 0;
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


    public class KalturaDrmEntryPlayingPluginData {
        String scheme;
        String certificate;
        String licenseURL;

        public String getLicenseURL() {
            return licenseURL;
        }
    }
}
