package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.annotations.SerializedName;
import com.kaltura.playkit.backend.BaseResult;

/**
 * @hide
 */

public class AssetResult extends BaseResult {

    @SerializedName(value = "result")
    public KalturaMediaAsset asset;

}
