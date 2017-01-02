package com.connect.backend.magikapp.services;

import com.connect.utils.RequestBuilder;

/**
 * Created by tehilarozin on 01/01/2017.
 */

public class ConfigurationService {
    // https://preprod-mediago.s3.amazonaws.com/magikapp/magik-app-demo/config.json // appId: magik-app-demo
    public static String ConfigBaseUrl = "https://preprod-mediago.s3.amazonaws.com/magikapp/";

    public static RequestBuilder fetch(String appId){
        return new RequestBuilder()
                .method("GET")
                .url(getConfigUrl(appId))
                .tag("configurations");
    }

    private static String getConfigUrl(String appId){
        StringBuilder configUrl = new StringBuilder();
        configUrl.append(ConfigBaseUrl).append(appId).append("/config.json");
        return configUrl.toString();
    }

}
