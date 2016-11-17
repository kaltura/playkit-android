package com.kaltura.playkit;

public class PKDrmParams {
    private String licenseUri;

    public PKDrmParams(String licenseUrl){
        this.licenseUri = licenseUrl;
    }

    public String getLicenseUri() {
        return licenseUri;
    }

    public void setLicenseUri(String licenseUri) {
        this.licenseUri = licenseUri;
    }
}
