/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.kaltura.playkit.player.MediaSupport;

public class PKDrmParams implements Parcelable {

    public enum Scheme {

        WidevineCENC,
        PlayReadyCENC,
        WidevineClassic,
        PlayReadyClassic,
        FairPlay,
        Unknown;

        @Nullable
        private Boolean supported;

        public boolean isSupported() {
            if (supported == null) {
                switch (this) {
                    case WidevineCENC:
                        supported = MediaSupport.widevineModular();
                        break;
                    case PlayReadyCENC:
                        supported = MediaSupport.playready();
                        break;
                    case WidevineClassic:
                        supported = MediaSupport.widevineClassic();
                        break;
                    case PlayReadyClassic:
                    case FairPlay:
                    case Unknown:
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

    public PKDrmParams(String licenseUrl, Scheme scheme) {
        this.licenseUri = licenseUrl;
        this.scheme = scheme;
    }

    public boolean isSchemeSupported() {
        return (scheme != null && scheme.isSupported());
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.licenseUri);
        dest.writeInt(this.scheme == null ? -1 : this.scheme.ordinal());
    }

    protected PKDrmParams(Parcel in) {
        this.licenseUri = in.readString();
        int tmpScheme = in.readInt();
        this.scheme = tmpScheme == -1 ? Scheme.Unknown : Scheme.values()[tmpScheme];
    }

    public static final Creator<PKDrmParams> CREATOR = new Creator<PKDrmParams>() {
        @Override
        public PKDrmParams createFromParcel(Parcel source) {
            return new PKDrmParams(source);
        }

        @Override
        public PKDrmParams[] newArray(int size) {
            return new PKDrmParams[size];
        }
    };
}
