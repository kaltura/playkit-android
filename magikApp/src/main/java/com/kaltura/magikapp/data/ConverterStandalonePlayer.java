package com.kaltura.magikapp.data;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterStandalonePlayer {

    public enum MediaProviderTypes {
        PHOENIX_MEDIA_PROVIDER,
        KALTURA_OVP_MEDIA_PROVIDER,
        MOCK_MEDIA_PROVIDER
    }


    ConverterMediaProvider mediaProvider;
    ConverterPlayerConfig playerConfig;


    public ConverterMediaProvider getMediaProvider() {
        return mediaProvider;
    }


    public ConverterPlayerConfig getPlayerConfig() {
        return playerConfig;
    }

}
