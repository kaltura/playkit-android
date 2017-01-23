package com.kaltura.playkit;

import static com.kaltura.playkit.PKDrmParams.Scheme.playready_cenc;
import static com.kaltura.playkit.PKDrmParams.Scheme.widevine_cenc;
import static com.kaltura.playkit.PKDrmParams.Scheme.widevine_classic;

public class PKDrmParams {

    public static enum Scheme {

        widevine_cenc,
        playready_cenc,
        widevine_classic
    }

    public static Scheme getSchemeEnumByName(String code) {

        switch (code) {
            case "drm.WIDEVINE_CENC":
                return widevine_cenc;
            case "drm.PLAYREADY_CENC":
                return playready_cenc;
            case "drm.WIDEVINE_CLASSIC":
                return widevine_classic;
            default:
                return null;
        }
    }

    private String licenseUri;
    private Scheme scheme;

    public PKDrmParams(String licenseUrl, String scheme){
        this.licenseUri = licenseUrl;
        this.scheme = getSchemeEnumByName(scheme);
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
