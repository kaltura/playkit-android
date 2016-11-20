package com.kaltura.playkitdemo.jsonConverters;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterPlugins {


    String pluginName;

    // IMA plugin
    String adTagUrl;
    String language;
    // add more configurations of IMA here


    // Youbora plugin
    String youboraKey;
    boolean adsAnalytics;
    // add more configurations of Youbora here


    // add new plugins configurations here


    public boolean isAdsAnalytics() {
        return adsAnalytics;
    }

    public String getAdTagUrl() {
        return adTagUrl;
    }

    public String getLanguage() {
        return language;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getYouboraKey() {
        return youboraKey;
    }
}
