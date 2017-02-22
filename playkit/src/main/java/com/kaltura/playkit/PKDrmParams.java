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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKDrmParams drmParams = (PKDrmParams) o;

        if (licenseUri != null ? !licenseUri.equals(drmParams.licenseUri) : drmParams.licenseUri != null)
            return false;
        return scheme == drmParams.scheme;

    }

    @Override
    public int hashCode() {
        int result = licenseUri != null ? licenseUri.hashCode() : 0;
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        return result;
    }
}
