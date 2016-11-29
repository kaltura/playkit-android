package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.annotations.SerializedName;
import com.kaltura.playkit.backend.BaseResult;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetResult extends BaseResult {

    @SerializedName(value = "result")
    public KalturaMediaAsset asset;

}
