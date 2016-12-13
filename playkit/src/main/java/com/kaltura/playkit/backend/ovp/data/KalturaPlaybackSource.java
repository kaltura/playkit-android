package com.kaltura.playkit.backend.ovp.data;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaPlaybackSource {

    private int deliveryProfileId;
    private String format;
    private String url;
    private String protocols; // currently list of KalturaString objects
    private List<String> flavors; //not clear if should be list<String> or string, currently list of KalturaString objects
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
            return protocols.substring(0, protocols.indexOf(","));
        }

        return null;
    }
    /*public String getProtocol(String preferred) {
        if (protocols != null && protocols.size() > 0) {
            for (KalturaValue kalturaValue : protocols) {
                if (kalturaValue.getValue().equals(preferred)) return preferred;
            }
            return (String) protocols.get(0).getValue();
        }

        return null;
    }*/

    public List<String> getFlavors() {
        return flavors;
    }

    public boolean hasFlavors() {
        return flavors != null && flavors.size() > 0;
    }

    public String getFormat() {
        return format;
    }

    public String getUrl() {
        return url;
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
