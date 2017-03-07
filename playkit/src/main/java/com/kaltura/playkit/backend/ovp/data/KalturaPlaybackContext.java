package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.base.data.BasePlaybackContext;

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
