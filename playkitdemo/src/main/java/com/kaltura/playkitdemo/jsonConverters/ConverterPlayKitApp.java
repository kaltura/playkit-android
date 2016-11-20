package com.kaltura.playkitdemo.jsonConverters;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 19/11/2016.
 */

public class ConverterPlayKitApp implements Parcelable {

    List<ConverterRootMenu> playKitApp;


    public List<ConverterRootMenu> getConverterRootMenuList() {
        return playKitApp;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.playKitApp);
    }

    public ConverterPlayKitApp() {
    }

    protected ConverterPlayKitApp(Parcel in) {
        this.playKitApp = new ArrayList<ConverterRootMenu>();
        in.readList(this.playKitApp, ConverterRootMenu.class.getClassLoader());
    }

    public static final Parcelable.Creator<ConverterPlayKitApp> CREATOR = new Parcelable.Creator<ConverterPlayKitApp>() {
        @Override
        public ConverterPlayKitApp createFromParcel(Parcel source) {
            return new ConverterPlayKitApp(source);
        }

        @Override
        public ConverterPlayKitApp[] newArray(int size) {
            return new ConverterPlayKitApp[size];
        }
    };
}
