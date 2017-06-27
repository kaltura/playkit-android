package com.kaltura.playkit.plugins.ott;

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
    private JsonObject initObject;

    public TVPAPIAnalyticsConfig(String baseUrl, int timerInterval, JsonObject initObject) {
        this.baseUrl = baseUrl;
        this.timerInterval = timerInterval;
        this.initObject = initObject;
    }

    public TVPAPIAnalyticsConfig setInitObject(JsonObject initObject) {
        this.initObject = initObject;
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

    public JsonObject getInitObject() {
        return initObject;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimerInterval() {
        return timerInterval;
    }
}
