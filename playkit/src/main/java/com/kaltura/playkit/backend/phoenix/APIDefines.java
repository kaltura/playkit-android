package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.StringDef;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * @hide
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
    @StringDef(value = {BookmarkType.Media, BookmarkType.Epg, BookmarkType.Recording})
    public @interface BookmarkType {
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

}

