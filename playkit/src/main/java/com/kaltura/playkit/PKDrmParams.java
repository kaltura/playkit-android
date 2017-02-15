package com.kaltura.playkit;

import com.kaltura.playkit.backend.phoenix.data.DrmScheme;

public class PKDrmParams {

    /*public static enum Scheme {

        widevine_cenc,
        playready_cenc,
        widevine_classic
    }*/

    private String licenseUri;
    private DrmScheme scheme;

    public PKDrmParams(String licenseUrl, DrmScheme scheme){
        this.licenseUri = licenseUrl;
        this.scheme = scheme;
    }

    public String getLicenseUri() {
        return licenseUri;
    }

    public void setLicenseUri(String licenseUri) {
        this.licenseUri = licenseUri;
    }

    public DrmScheme getScheme() {
        return scheme;
    }

    public void setScheme(DrmScheme scheme) {
        this.scheme = scheme;
    }
}
