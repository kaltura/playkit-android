package com.kaltura.playkit.plugins.configs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class TVPAPIAnalyticsConfig {
    public static final String BASE_URL       = "baseUrl";
    public static final String TIMER_INTERVAL = "timerInterval";
    public static final String INIT_OBJ       = "initObj";

    private String baseUrl;
    private int timerInterval;
    private TVPAPIInitObject initObj;

    public TVPAPIAnalyticsConfig(String baseUrl, int timerInterval, TVPAPIInitObject initObj) {
        this.baseUrl = baseUrl;
        this.timerInterval = timerInterval;
        this.initObj = initObj;
    }

    public TVPAPIAnalyticsConfig setInitObj(TVPAPIInitObject initObj) {
        this.initObj = initObj;
        return this;
    }


    public TVPAPIAnalyticsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public TVPAPIAnalyticsConfig setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
        return this;
    }

    public TVPAPIInitObject getInitObj() {
        return initObj;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimerInterval() {
        return timerInterval;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(TIMER_INTERVAL, timerInterval);

        JsonElement element = new Gson().toJsonTree(initObj.toJsonObject());
        JsonObject object = element.getAsJsonObject();
        jsonObject.add(INIT_OBJ, object);
        return jsonObject;
    }
}
