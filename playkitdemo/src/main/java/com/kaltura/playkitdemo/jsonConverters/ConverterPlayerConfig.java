package com.kaltura.playkitdemo.jsonConverters;

import java.util.List;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterPlayerConfig {

    ConverterMedia media;
    List<ConverterPlugins> plugins;


    public ConverterMedia getMedia() {
        return media;
    }

    public List<ConverterPlugins> getPlugins() {
        return plugins;
    }
}
