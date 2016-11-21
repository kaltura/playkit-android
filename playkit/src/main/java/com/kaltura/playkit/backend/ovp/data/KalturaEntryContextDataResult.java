package com.kaltura.playkit.backend.ovp.data;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaEntryContextDataResult {

    ArrayList<KalturaSource> sources;
    ArrayList<FlavorAsset> flavorAssets;

    public ArrayList<FlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

    public FlavorAsset containsFlavor(int flavorId) {
        if(flavorAssets == null || flavorAssets.size() == 0){
            return null;
        }

        for(FlavorAsset flavorAsset : flavorAssets){
            if(flavorAsset.getFlavorParamsId() == flavorId){
                return flavorAsset;
            }
        }

        return null;
    }

    public ArrayList<KalturaSource> getSources() {
        return sources;
    }

}
