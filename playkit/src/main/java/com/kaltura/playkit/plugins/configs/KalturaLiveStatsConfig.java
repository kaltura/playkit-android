package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaLiveStatsConfig {


    public static final String PARTNER_ID = "partnerId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";
    public static final String USER_ID    = "userId";
    public static final String CONTEXT_ID = "contextId";


    private int partnerId;
    private String entryId;
    private String baseUrl;
    private String userId;
    private int contextId;

    public KalturaLiveStatsConfig(int partnerId, String entryId, String userId, int contextId) {
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.entryId = entryId;
        this.userId = userId;
        this.contextId = contextId;
    }

    public KalturaLiveStatsConfig(int partnerId, String entryId, String baseUrl, String userId, int contextId) {
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.entryId = entryId;
        this.userId = userId;
        this.contextId = contextId;
    }

    public KalturaLiveStatsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KalturaLiveStatsConfig setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public KalturaLiveStatsConfig setContextId(int contextId) {
        this.contextId = contextId;
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

    public String getUserId() {
        return userId;
    }

    public int getContextId() {
        return contextId;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(USER_ID, userId);
        jsonObject.addProperty(CONTEXT_ID, contextId);

        return jsonObject;
    }
}
