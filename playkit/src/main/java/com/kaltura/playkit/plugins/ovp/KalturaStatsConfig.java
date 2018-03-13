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
import com.kaltura.playkit.utils.Consts;

/**
 * Created by gilad.nadav on 18/05/2017.
 */

public class KalturaStatsConfig {


    public static final String PARTNER_ID = "partnerId";
    public static final String UICONF_ID  = "uiconfId";
    public static final String ENTRY_ID   = "entryId";
    public static final String BASE_URL   = "baseUrl";
    public static final String USER_ID    = "userId";
    public static final String CONTEXT_ID = "contextId";
    public static final String TIMER_INTERVAL = "timerInterval"; //in seconds
    public static final String HAS_KANALONY = "hasKanalony";

    private int partnerId;
    private int uiconfId;
    private String entryId;
    private String baseUrl;
    private String userId;
    private int contextId;
    private int timerInterval;
    private boolean hasKanalony;

    /* hasKanalony should be provided so we can differ between :
     * hybrid mode of kalturaAnalytics + kavaAnalytics (hasKanalony=true)
     * and katuraAnalytics only (hasKanalony=false)
    */
    public KalturaStatsConfig(boolean hasKanalony) {
        this.hasKanalony = hasKanalony;
    }

    public KalturaStatsConfig(int uiconfId, int partnerId, String entryId, String userId, int contextId, boolean hasKanalony) {
        this.timerInterval = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW_SEC;
        this.baseUrl = "https://stats.kaltura.com/api_v3/index.php";
        this.partnerId = partnerId;
        this.uiconfId = uiconfId;
        this.entryId = entryId;
        this.userId = userId;
        this.contextId = contextId;
        this.hasKanalony = hasKanalony;
    }

    public KalturaStatsConfig(int uiconfId, int partnerId, String entryId, String baseUrl, String userId, int contextId, int timerInterval, boolean hasKanalony) {
        this.timerInterval = timerInterval;
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.uiconfId = uiconfId;
        this.entryId = entryId;
        this.userId = userId;
        this.contextId = contextId;
        this.hasKanalony = hasKanalony;
    }

    public KalturaStatsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KalturaStatsConfig setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public KalturaStatsConfig setContextId(int contextId) {
        this.contextId = contextId;
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

    public KalturaStatsConfig setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
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

    public int getTimerInterval() {
        return timerInterval;
    }

    public String getUserId() {
        return userId;
    }

    public int getContextId() {
        return contextId;
    }

    public boolean getHasKanalony() {
        return hasKanalony;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(UICONF_ID, uiconfId);
        jsonObject.addProperty(ENTRY_ID, entryId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(USER_ID, userId);
        jsonObject.addProperty(CONTEXT_ID, contextId);
        jsonObject.addProperty(TIMER_INTERVAL, timerInterval);
        jsonObject.addProperty(HAS_KANALONY, hasKanalony);

        return jsonObject;
    }
}
