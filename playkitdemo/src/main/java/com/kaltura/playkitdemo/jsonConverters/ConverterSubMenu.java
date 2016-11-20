package com.kaltura.playkitdemo.jsonConverters;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 19/11/2016.
 */

public class ConverterSubMenu implements Parcelable {

    String subMenuTitle;
    List<ConverterFeatureVariants> featureVariants;
    String aboutFeature;
    String featureId;


    public String getAboutFeature() {
        return aboutFeature;
    }

    public String getFeatureId() {
        return featureId;
    }

    public List<ConverterFeatureVariants> getFeatureVariants() {
        return featureVariants;
    }

    public String getSubMenuTitle() {
        return subMenuTitle;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.subMenuTitle);
        dest.writeList(this.featureVariants);
        dest.writeString(this.aboutFeature);
        dest.writeString(this.featureId);
    }

    public ConverterSubMenu() {
    }

    protected ConverterSubMenu(Parcel in) {
        this.subMenuTitle = in.readString();
        this.featureVariants = new ArrayList<ConverterFeatureVariants>();
        in.readList(this.featureVariants, ConverterFeatureVariants.class.getClassLoader());
        this.aboutFeature = in.readString();
        this.featureId = in.readString();
    }

    public static final Parcelable.Creator<ConverterSubMenu> CREATOR = new Parcelable.Creator<ConverterSubMenu>() {
        @Override
        public ConverterSubMenu createFromParcel(Parcel source) {
            return new ConverterSubMenu(source);
        }

        @Override
        public ConverterSubMenu[] newArray(int size) {
            return new ConverterSubMenu[size];
        }
    };
}
