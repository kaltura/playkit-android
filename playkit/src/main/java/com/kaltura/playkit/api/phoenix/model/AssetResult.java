package com.kaltura.playkit.api.phoenix.model;

import com.google.gson.annotations.SerializedName;
import com.kaltura.netkit.connect.response.BaseResult;

/**
 * @hide
 */

public class AssetResult extends BaseResult {

    @SerializedName(value = "result")
    public KalturaMediaAsset asset;

}
