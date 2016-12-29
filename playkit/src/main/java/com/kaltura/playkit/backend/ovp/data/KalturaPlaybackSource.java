package com.kaltura.playkit.backend.ovp.data;

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
     * in case the supported protocols contains the defined preferable protocol return it
     * otherwise returns the first protocol in the list.
     *
     * @param preferred see {@link com.kaltura.playkit.backend.ovp.OvpConfigs#PreferredHttpProtocol}
     * @return
     */
    public String getProtocol(String preferred) {
        if (protocols != null && protocols.length() > 0) {
            if (protocols.contains(preferred)) {
                return preferred;
            }
            int endIndex = protocols.indexOf(",");
            return protocols.substring(0, endIndex >= 0 ? endIndex : protocols.length());
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
