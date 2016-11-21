package com.kaltura.playkit.backend.ovp.data;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaSource {

    int deliveryProfileId;
    String format;
    String url;
    List<Drm> drmData;
    List<String> flavors;

    public int getId() {
        return deliveryProfileId;
    }

    public String getUrl() {
        return url;
    }

    public List<Drm> getDrmData() {
        return drmData;
    }

    public class Drm{
        String scheme;
        String certificate;
        String licenseURL;

        public String getLicenseURL(){
            return licenseURL;
        }
    }
}
