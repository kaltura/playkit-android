package com.kaltura.playkit.api.phoenix.model;

import android.support.annotation.StringDef;

import com.kaltura.playkit.api.base.model.BasePlaybackSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.kaltura.playkit.api.phoenix.model.KalturaPlaybackSource.SourceMetadata.AdsPolicy;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class KalturaPlaybackSource extends BasePlaybackSource {
    private int assetId;
    private int id;
    private String type; //Device types as defined in the system (MediaFileFormat)
    private long duration;
    private String externalId;
    private String adsPolicy;

    public int getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }

    public String getType() {
        return type;
    }

    public String getAdsPolicy() {
        return adsPolicy;
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({AdsPolicy})
    public @interface SourceMetadata {
        String AdsPolicy = "adsPolicy";
    }

}
