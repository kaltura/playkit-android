package com.kaltura.playkit.backend.ovp.data;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaPlayingSource {

    int deliveryProfileId;
    String format;
    List<String> flavors;
    List<String> protocols;
    List<Drm> drm;
    //Map<String, Drm> drm;


    public int getDeliveryProfileId() {
        return deliveryProfileId;
    }

    public List<Drm>/*Map<String, Drm>*/ getDrmData() {
        return drm;
    }

    public String getProtocol(String preferred){
        if(protocols != null && protocols.size()>0){
            if(protocols.contains(preferred)){
                return preferred;
            }
            return protocols.get(0);
        }

        return null;
    }

    public List<String> getFlavors() {
        return flavors;
    }

    public String getFormat() {
        return format;
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
