package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;

import static com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType.ExternalEpg;
import static com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType.InternalEpg;
import static com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType.Media;
import static com.kaltura.playkit.backend.phoenix.APIDefines.BookmarkType.Epg;
import static com.kaltura.playkit.backend.phoenix.APIDefines.BookmarkType.Recording;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 20/11/2016.
 */

public class APIDefines {

    @Retention(SOURCE)
    @StringDef(value = {
            Media,
            InternalEpg,
            ExternalEpg
    })

    public @interface AssetReferenceType {
        String Media = "media";
        String InternalEpg = "epg_internal";
        String ExternalEpg = "epg_external";
    }

    @Retention(SOURCE)
    @StringDef(value = {
            Media,
            Epg,
            Recording
    })

    public @interface BookmarkType {
        String Media = "media";
        String Epg = "epg";
        String Recording = "recording";
    }





}
