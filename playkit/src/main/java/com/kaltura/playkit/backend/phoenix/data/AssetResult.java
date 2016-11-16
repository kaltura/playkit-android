package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetResult extends BaseResult {

    @SerializedName(value = "result")
    public AssetInfo asset;

}
