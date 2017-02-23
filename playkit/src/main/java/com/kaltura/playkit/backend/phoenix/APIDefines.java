package com.kaltura.playkit.backend.phoenix;

/**
 * @hide
 */

public class APIDefines {



    public enum AssetReferenceType {
        Media("media"),
        InternalEpg("epg_internal"),
        ExternalEpg("epg_external");

        AssetReferenceType(String value){
            this.value = value;
        }

        public String value;

    }

    public enum KalturaAssetType {
        Media("media"),
        Epg("epg"),
        Recording("recording");

        KalturaAssetType(String value){
            this.value = value;
        }

        public String value;
    }


    public enum LiveStreamType {
        Catchup("catchup"),
        StartOver("startOver"),
        TrickPlay("trickPlay");

        LiveStreamType(String value){
            this.value = value;
        }

        public String value;
    }


    public enum MediaType {
        Vod("vod"),
        Channel("channel"),
        Program("program"),
        EPG("epg");

        MediaType(String value){
            this.value = value;
        }

        public String value;
    }

    public enum PlaybackContextType {
        Trailer("TRAILER"),
        Catchup("CATCHUP"),
        StartOver("START_OVER"),
        Playback("PLAYBACK");

        PlaybackContextType(String value){
            this.value = value;
        }

        public String value;
    }

}

