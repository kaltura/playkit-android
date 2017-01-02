package com.kaltura.magikapp.data;

import java.util.List;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterPlayerConfig {

    ConverterMedia media;
    List<ConverterPlugin> plugins;
    List<ConverterAddon> addons;


    public ConverterMedia getMedia() {
        return media;
    }


    public List<ConverterPlugin> getPlugins() {
        return plugins;
    }


    public List<ConverterAddon> getAddons() {
        return addons;
    }


    public ConverterAddon getAddon(ConverterAddon.AddonType addonType) {

        if (addons != null) { // addon configuration in json isn't mandatory

            for (ConverterAddon converterAddon : addons) {

                if (converterAddon.getAddonType() == addonType) {

                    return converterAddon;

                }
            }
        }

        return null;
    }

}
