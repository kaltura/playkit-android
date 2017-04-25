package com.kaltura.playkit.api.ovp.model;

import com.kaltura.playkit.api.base.model.BasePlaybackContext;

import java.util.ArrayList;

/**
 * @hide
 */

public class KalturaPlaybackContext extends BasePlaybackContext{

    private ArrayList<KalturaPlaybackSource> sources;
    private ArrayList<KalturaFlavorAsset> flavorAssets;

    public KalturaPlaybackContext() {
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

}
