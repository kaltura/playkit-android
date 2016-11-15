package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tehilarozin on 15/11/2016.
 */

public class StamResult extends BaseResult {
    @SerializedName(value = "result")
    public MediaFile file;
}
