package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.StringDef;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 20/11/2016.
 */

public class APIDefines {

    @Retention(SOURCE)
    @StringDef(value = {AssetReferenceType.Media, AssetReferenceType.InternalEpg, AssetReferenceType.ExternalEpg})
    public @interface AssetReferenceType {
        String Media = "media";
        String InternalEpg = "epg_internal";
        String ExternalEpg = "epg_external";
    }

    @Retention(SOURCE)
    @StringDef(value = {KalturaAssetType.Media, KalturaAssetType.Epg, KalturaAssetType.Recording})
    public @interface KalturaAssetType {
        String Media = "media";
        String Epg = "epg";
        String Recording = "recording";
    }


    @Retention(SOURCE)
    @StringDef(value = {LiveStreamType.Catchup, LiveStreamType.StartOver, LiveStreamType.TrickPlay})
    public @interface LiveStreamType {
        String Catchup = "catchup";
        String StartOver = "startOver";
        String TrickPlay = "trickPlay";
    }


    @Retention(SOURCE)
    @StringDef(value = {MediaType.Vod, MediaType.Channel, MediaType.Program})
    public @interface MediaType {
        String Vod = "vod";
        String Channel = "channel";
        String Program = "program";
        String EPG = "epg";
    }

    @Retention(SOURCE)
    @StringDef(value = {PlaybackContextType.Trailer, PlaybackContextType.Catchup, PlaybackContextType.StartOver, PlaybackContextType.Playback})
    public @interface PlaybackContextType {
        String Trailer = "TRAILER";
        String Catchup = "CATCHUP";
        String StartOver = "START_OVER";
        String Playback = "PLAYBACK";
    }

}

