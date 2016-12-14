package com.kaltura.playkit.backend.ovp.data;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlaybackContext extends KalturaEntryContextDataResult {

    ArrayList<KalturaPlaybackSource> sources;

    public KalturaPlaybackContext() {
    }

    public KalturaPlaybackContext(KalturaEntryContextDataResult contextDataResult) {
        flavorAssets = contextDataResult.flavorAssets;
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

}
