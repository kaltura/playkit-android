package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaEntryContextDataResult extends BaseResult {

    ArrayList<KalturaSource> sources;
    ArrayList<KalturaFlavorAsset> flavorAssets;

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

    public KalturaFlavorAsset containsFlavor(int flavorId) {
        if(flavorAssets == null || flavorAssets.size() == 0){
            return null;
        }

        for(KalturaFlavorAsset flavorAsset : flavorAssets){
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
