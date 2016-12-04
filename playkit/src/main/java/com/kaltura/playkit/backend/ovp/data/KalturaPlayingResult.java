package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlayingResult extends BaseResult {

    ArrayList<KalturaPlayingSource> sources;
    ArrayList<KalturaFlavorAsset> flavorAssets;


    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }


    public ArrayList<KalturaPlayingSource> getSources() {
        return sources;
    }

}
