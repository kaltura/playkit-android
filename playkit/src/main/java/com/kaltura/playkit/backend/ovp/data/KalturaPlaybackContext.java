package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.base.data.BasePlaybackContext;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlaybackContext extends BasePlaybackContext{

    ArrayList<KalturaPlaybackSource> sources;
    ArrayList<KalturaFlavorAsset> flavorAssets;

    public KalturaPlaybackContext() {
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

}
