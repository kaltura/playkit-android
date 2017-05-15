package com.kaltura.playkit.api.ovp.model;

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.ArrayList;

/**
 * @hide
 */

public class KalturaEntryContextDataResult extends BaseResult {

    ArrayList<KalturaFlavorAsset> flavorAssets;


    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }


}
