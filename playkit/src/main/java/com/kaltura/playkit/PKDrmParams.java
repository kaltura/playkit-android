package com.kaltura.playkit;

public class PKDrmParams {

    public static enum Scheme {

        widevine_cenc,
        playready_cenc,
        widevine_classic
    }

    private String licenseUri;
    private Scheme scheme;

    public PKDrmParams(String licenseUrl, Scheme scheme){
        this.licenseUri = licenseUrl;
        this.scheme = scheme;
    }

    public String getLicenseUri() {
        return licenseUri;
    }

    public void setLicenseUri(String licenseUri) {
        this.licenseUri = licenseUri;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme scheme) {
        this.scheme = scheme;
    }
}
