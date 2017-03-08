package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

import com.kaltura.playkit.player.MediaSupport;

public class PKDrmParams implements Parcelable {

    public enum Scheme {

        WidevineCENC(MediaSupport.widevineModular()),
        PlayReadyCENC(MediaSupport.playready()),
        WidevineClassic(MediaSupport.widevineClassic()),
        PlayReadyClassic(false),
        FairPlay(false),
        Unknown(false);

        public boolean isSupported() {
            return supported;
        }

        private final boolean supported;

        Scheme(boolean supported) {
            this.supported = supported;
        }
    }

    private String licenseUri;
    private Scheme scheme = Scheme.Unknown;

    public PKDrmParams(String licenseUrl, Scheme scheme){
        this.licenseUri = licenseUrl;
        this.scheme = scheme;
    }

    protected PKDrmParams(Parcel in) {
        licenseUri = in.readString();
        scheme = Utils.byValue(Scheme.class, in.readString(), Scheme.Unknown);//Scheme.valueOf(in.readString());
    }

    public boolean isSchemeSupported() {
        return scheme != null && scheme.isSupported();
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
