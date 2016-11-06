package com.kaltura.playkit.plugin.mediaprovider.ovp.data;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaEntryContextDataResult {

    ArrayList<EntrySource> sources;
    ArrayList<FlavorAsset> flavorAssets;

    public ArrayList<FlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

    public void setFlavorAssets(ArrayList<FlavorAsset> flavorAssets) {
        this.flavorAssets = flavorAssets;
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
}
