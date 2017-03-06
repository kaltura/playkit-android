package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

public class PKDrmParams implements Parcelable {

    public enum Scheme {

        widevine_cenc,
        playready_cenc,
        widevine_classic,
        playready,
        fairplay,
        none
    }

    private String licenseUri;
    private Scheme scheme = Scheme.none;

    public PKDrmParams(String licenseUrl, Scheme scheme){
        this.licenseUri = licenseUrl;
        this.scheme = scheme;
    }

    protected PKDrmParams(Parcel in) {
        licenseUri = in.readString();
        scheme = Utils.byValue(Scheme.class, in.readString(), Scheme.none);//Scheme.valueOf(in.readString());
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

    public static final Creator<PKDrmParams> CREATOR = new Creator<PKDrmParams>() {
        @Override
        public PKDrmParams createFromParcel(Parcel in) {
            return new PKDrmParams(in);
        }

        @Override
        public PKDrmParams[] newArray(int size) {
            return new PKDrmParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(licenseUri);
        dest.writeString(scheme.name());
    }
}
