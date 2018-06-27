/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ott;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class PhoenixAnalyticsConfig {
    public static final String PARTNER_ID = "partnerId";
    public static final String BASE_URL   = "baseUrl";
    public static final String KS         = "ks";
    public static final String TIMER_INTERVAL = "timerInterval";

    private int partnerId;
    private String baseUrl;
    private String ks;
    private int timerInterval;

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerInterval) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerInterval = timerInterval;
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

    public PhoenixAnalyticsConfig setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
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

    public int getTimerInterval() {
        return timerInterval;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(KS, ks);
        jsonObject.addProperty(TIMER_INTERVAL, timerInterval);

        return jsonObject;
    }
}
