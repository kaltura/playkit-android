package com.kaltura.playkitdemo.jsonConverters;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by itanbarpeled on 19/11/2016.
 */

public class ConverterFeatureVariants implements Parcelable {

    String featureTitle;
    String playerConfigLink;


    public String getFeatureTitle() {
        return featureTitle;
    }

    public String getPlayerConfigLink() {
        return playerConfigLink;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.featureTitle);
        dest.writeString(this.playerConfigLink);
    }

    public ConverterFeatureVariants() {
    }

    protected ConverterFeatureVariants(Parcel in) {
        this.featureTitle = in.readString();
        this.playerConfigLink = in.readString();
    }

    public static final Parcelable.Creator<ConverterFeatureVariants> CREATOR = new Parcelable.Creator<ConverterFeatureVariants>() {
        @Override
        public ConverterFeatureVariants createFromParcel(Parcel source) {
            return new ConverterFeatureVariants(source);
        }

        @Override
        public ConverterFeatureVariants[] newArray(int size) {
            return new ConverterFeatureVariants[size];
        }
    };
}
