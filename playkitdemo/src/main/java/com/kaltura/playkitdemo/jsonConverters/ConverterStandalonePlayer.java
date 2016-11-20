package com.kaltura.playkitdemo.jsonConverters;

import com.google.gson.annotations.Expose;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterStandalonePlayer {


    public enum MediaProviderTypes {
        PHOENIX_MEDIA_PROVIDER, KALTURA_OVP_MEDIA_PROVIDER
    }

    ConverterPhoenixMediaProvider phoenixMediaProvider;
    ConverterPlayerConfig playerConfig;
    ConverterKalturaOvpMediaProvider kalturaOvpMediaProvider;

    public ConverterPhoenixMediaProvider getPhoenixMediaProvider() {
        return phoenixMediaProvider;
    }

    public ConverterPlayerConfig getPlayerConfig() {
        return playerConfig;
    }

    public ConverterKalturaOvpMediaProvider getKalturaOvpMediaProvider() {
        return kalturaOvpMediaProvider;
    }
}
