package com.kaltura.playkit.mediaproviders.phoenix.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetResult {

    public double executionTime;
    @SerializedName(value = "result")
    public AssetInfo asset;
}
