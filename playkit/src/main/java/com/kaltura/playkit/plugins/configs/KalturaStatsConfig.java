package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaStatsConfig {
    private static final int DEFAULT_TIMER_INTERVAL_MILLIS = 30000;

    public static final String PARTNER_ID = "partnerId";
    public static final String UICONF_ID  = "uiconfId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";
    public static final String TIMER_INTERVAL_MILLIS = "timerIntervalMillis";

    private String partnerId;
    private String uiconfId;
    private String entryId;
    private String baseUrl;
    private int timerIntervalMillis;

    public KalturaStatsConfig(String uiconfId, String partnerId, String entryId) {
        this.timerIntervalMillis = DEFAULT_TIMER_INTERVAL_MILLIS;
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.uiconfId = uiconfId;
        this.entryId = entryId;
    }

    public KalturaStatsConfig setPartnerId(String partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KalturaStatsConfig setUiconfId(String uiconfId) {
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

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(UICONF_ID, uiconfId);
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(TIMER_INTERVAL_MILLIS, timerIntervalMillis);

        return jsonObject;
    }
}