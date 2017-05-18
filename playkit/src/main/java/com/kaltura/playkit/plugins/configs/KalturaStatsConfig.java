package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaStatsConfig {


    public static final String PARTNER_ID = "partnerId";
    public static final String UICONF_ID  = "uiconfId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";
    public static final String TIMER_INTERVAL_MILLIS = "timerIntervalMillis";

    private int partnerId;
    private int uiconfId;
    private String entryId;
    private String baseUrl;
    private int timerIntervalMillis;

    public KalturaStatsConfig(int uiconfId, int partnerId, String entryId) {
        this.timerIntervalMillis = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW;
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.uiconfId = uiconfId;
        this.entryId = entryId;
    }

    public KalturaStatsConfig(int uiconfId, int partnerId, String entryId, String baseUrl, int timerIntervalMillis) {
        this.timerIntervalMillis = timerIntervalMillis;
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.uiconfId = uiconfId;
        this.entryId = entryId;
    }

    public KalturaStatsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KalturaStatsConfig setUiconfId(int uiconfId) {
        this.uiconfId = uiconfId;
        return this;
    }

    public KalturaStatsConfig setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public KalturaStatsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public KalturaStatsConfig setTimerIntervalMillis(int timerIntervalMillis) {
        this.timerIntervalMillis = timerIntervalMillis;
        return this;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public int getUiconfId() {
        return uiconfId;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimerIntervalMillis() {
        return timerIntervalMillis;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(UICONF_ID, uiconfId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(TIMER_INTERVAL_MILLIS, timerIntervalMillis);

        return jsonObject;
    }
}
