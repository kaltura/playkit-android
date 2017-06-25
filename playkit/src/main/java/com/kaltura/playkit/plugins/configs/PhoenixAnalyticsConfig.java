package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class PhoenixAnalyticsConfig {
    public static final String PARTNER_ID = "partnerId";
    public static final String BASE_URL   = "baseUrl";
    public static final String KS    = "ks";
    public static final String TIMER_INTERVAL_SEC = "timerIntervalSec";

    private int partnerId;
    private String baseUrl;
    private String ks;
    private int timerIntervalSec;

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerIntervalSec) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerIntervalSec = timerIntervalSec;
    }



    public PhoenixAnalyticsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public PhoenixAnalyticsConfig setKS(String ks) {
        this.ks = ks;
        return this;
    }


    public PhoenixAnalyticsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public PhoenixAnalyticsConfig setTimerIntervalSec(int timerIntervalSec) {
        this.timerIntervalSec = timerIntervalSec;
        return this;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getKS() {
        return ks;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimerIntervalSec() {
        return timerIntervalSec;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(KS, ks);
        jsonObject.addProperty(TIMER_INTERVAL_SEC, timerIntervalSec);

        return jsonObject;
    }
}
