package com.kaltura.playkit.backend.phoenix.services;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixConfigs;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.MultiRequestBuilder;

/**
 * Created by tehilarozin on 14/11/2016.
 */
public class PhoenixService {

    public static ErrorElement getErrorElement(String code, String message){
        switch (code){
            case "500016":
                return ErrorElement.SessionError.message("session token has been expired");

            default:
                return ErrorElement.GeneralError.message(message);
        }
    }

    public static JsonObject getPhoenixConfigParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", PhoenixConfigs.ClientTag);
        params.addProperty("apiVersion",PhoenixConfigs.ApiVersion);

        return params;
    }

    public static MultiRequestBuilder getMultirequest(String baseUrl, @Nullable String ks){
        JsonObject params = getPhoenixConfigParams();
        if(!TextUtils.isEmpty(ks)) {
            params.addProperty("ks", ks);
        }
        return (MultiRequestBuilder) new MultiRequestBuilder().service("multirequest").method("POST").url(baseUrl).params(params);
    }
}
