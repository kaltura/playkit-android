package com.kaltura.magikapp.data;

/**
 * Created by itanbarpeled on 16/12/2016.
 */

public abstract class ConverterAddon {


    public enum AddonType {
        GOOGLE_CAST;
    }


    AddonType addonType;


    public AddonType getAddonType() {
        return addonType;
    }


    public void setAddonType(AddonType addonType) {
        this.addonType = addonType;
    }


}
