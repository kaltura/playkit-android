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

package com.kaltura.playkit.plugins.ovp;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaLiveStatsConfig {


    public static final String PARTNER_ID = "partnerId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";

    private int partnerId;
    private String entryId;
    private String baseUrl;

    public KalturaLiveStatsConfig(int partnerId, String entryId) {
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.entryId = entryId;
    }

    public KalturaLiveStatsConfig(int partnerId, String entryId, String baseUrl) {
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.entryId = entryId;
    }

    public KalturaLiveStatsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
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

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(BASE_URL, baseUrl);

        return jsonObject;
    }
}
