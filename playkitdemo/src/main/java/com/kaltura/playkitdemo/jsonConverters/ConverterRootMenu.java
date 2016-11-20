package com.kaltura.playkitdemo.jsonConverters;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 19/11/2016.
 */

public class ConverterRootMenu implements Parcelable {


    String rootMenuTitle;
    List<ConverterSubMenu> subMenu;


    public String getRootMenuTitle() {
        return rootMenuTitle;
    }

    public List<ConverterSubMenu> getSubMenu() {
        return subMenu;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.rootMenuTitle);
        dest.writeList(this.subMenu);
    }

    public ConverterRootMenu() {
    }

    protected ConverterRootMenu(Parcel in) {
        this.rootMenuTitle = in.readString();
        this.subMenu = new ArrayList<ConverterSubMenu>();
        in.readList(this.subMenu, ConverterSubMenu.class.getClassLoader());
    }

    public static final Parcelable.Creator<ConverterRootMenu> CREATOR = new Parcelable.Creator<ConverterRootMenu>() {
        @Override
        public ConverterRootMenu createFromParcel(Parcel source) {
            return new ConverterRootMenu(source);
        }

        @Override
        public ConverterRootMenu[] newArray(int size) {
            return new ConverterRootMenu[size];
        }
    };
}
