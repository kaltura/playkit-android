package com.kaltura.playkit.backend.ovp.data;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlayingResult extends KalturaEntryContextDataResult {

    ArrayList<KalturaPlayingSource> sources;

    public KalturaPlayingResult(KalturaEntryContextDataResult contextDataResult) {
        flavorAssets = contextDataResult.flavorAssets;
    }
    //ArrayList<KalturaFlavorAsset> flavorAssets;


   /* public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }*/


    public ArrayList<KalturaPlayingSource> getSources() {
        return sources;
    }

}
