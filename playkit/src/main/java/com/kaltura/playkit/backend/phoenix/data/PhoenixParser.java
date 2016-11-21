package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Created by tehilarozin on 15/11/2016.
 */

public class PhoenixParser {

    public static AssetResult parseAssetResult(String json) throws JsonSyntaxException {
        return new GsonBuilder().registerTypeAdapter(AssetResult.class, new ResultAdapter()).create().fromJson(json, AssetResult.class);
    }


}
