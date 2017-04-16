package com.kaltura.playkit.api.phoenix;

import com.kaltura.playkit.PKMediaEntry;

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
        Vod(KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Vod),
        Channel(KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Live),
        Recording(KalturaAssetType.Recording,PKMediaEntry.MediaEntryType.Vod),
        EPG(KalturaAssetType.Epg, PKMediaEntry.MediaEntryType.Live);

        MediaType(KalturaAssetType assetType, PKMediaEntry.MediaEntryType mediaEntryType){
            this.assetType = assetType;
            this.mediaEntryType = mediaEntryType;
        }

        private KalturaAssetType assetType;
        private PKMediaEntry.MediaEntryType mediaEntryType;

        public KalturaAssetType getAssetType() {
            return assetType;
        }

        public PKMediaEntry.MediaEntryType getMediaEntryType() {
            return mediaEntryType;
        }
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

