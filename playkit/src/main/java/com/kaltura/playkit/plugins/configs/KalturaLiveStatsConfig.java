package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaLiveStatsConfig {


    public static final String PARTNER_ID = "partnerId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";
    public static final String IS_DVR   = "isDVR";

    private int partnerId;
    private String entryId;
    private String baseUrl;
    private boolean isDVR;

    public KalturaLiveStatsConfig(int partnerId, String entryId, boolean isDVR) {
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.entryId = entryId;
        this.isDVR = isDVR;
    }

    public KalturaLiveStatsConfig(int partnerId, String entryId, String baseUrl, boolean isDVR) {
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.entryId = entryId;
        this.isDVR = isDVR;
    }

    public KalturaLiveStatsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KalturaLiveStatsConfig setIsDVR(boolean isDVR) {
        this.isDVR = isDVR;
        return this;
    }

    public KalturaLiveStatsConfig setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public KalturaLiveStatsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean getIsDVR() {
        return isDVR;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(IS_DVR, isDVR);
        return jsonObject;
    }
}
