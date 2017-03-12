package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;

import com.kaltura.playkit.player.MediaSupport;

public class PKDrmParams implements Parcelable {

    public enum Scheme {

        WidevineCENC,
        PlayReadyCENC,
        WidevineClassic,
        PlayReadyClassic,
        FairPlay,
        Unknown;

        private Boolean supported;
        public boolean isSupported() {
            if (supported == null) {
                switch (this) {
                    case WidevineCENC:
                        supported = MediaSupport.widevineModular();
                        break;
                    case PlayReadyCENC:
                        supported = MediaSupport.playReady();
                        break;
                    case WidevineClassic:
                        supported = MediaSupport.widevineClassic();
                        break;
                    case PlayReadyClassic:
                    case FairPlay:
                    case Unknown:
                        supported = false;
                        break;
                    default:
                        supported = false;
                        break;
                }
            }
            return supported;
            
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
