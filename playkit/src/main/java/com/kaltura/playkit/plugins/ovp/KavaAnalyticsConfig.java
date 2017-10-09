package com.kaltura.playkit.plugins.ovp;

import com.google.gson.JsonObject;
import com.kaltura.playkit.Utils;

/**
 * Created by anton.afanasiev on 04/10/2017.
 */

public class KavaAnalyticsConfig {

    private static final String DEFAULT_BASE_URL = "http://analytics.kaltura.com/api_v3/index.php";

    private static final String PARTNER_ID_KEY = "partnerId";
    private static final String UI_CONF_ID_KEY = "uiconfId";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String KS_KEY = "ks";

    private int uiconfId = 0;
    private int partnerId = 0;

    private String ks = "Unknown";
    private String baseUrl = DEFAULT_BASE_URL;


    public KavaAnalyticsConfig() {}

    public KavaAnalyticsConfig(JsonObject config) {
        if (Utils.isJsonObjectValueValid(config, UI_CONF_ID_KEY)) {
            uiconfId = Integer.valueOf(config.get(UI_CONF_ID_KEY).toString());
        } else {
            uiconfId = 0;
        }

        if (Utils.isJsonObjectValueValid(config, BASE_URL_KEY)) {
            baseUrl = config.getAsJsonPrimitive(BASE_URL_KEY).getAsString();
        } else {
            baseUrl = DEFAULT_BASE_URL;
        }

        if (Utils.isJsonObjectValueValid(config, PARTNER_ID_KEY)) {
            partnerId = config.getAsJsonPrimitive(PARTNER_ID_KEY).getAsInt();
        } else {
            partnerId = 0;
        }

        if (Utils.isJsonObjectValueValid(config, KS_KEY)) {
            ks = config.getAsJsonPrimitive(KS_KEY).getAsString();
        } else {
            ks = "Unknown";
        }
    }


    public KavaAnalyticsConfig setUiConfId(int uiConfId) {
        this.uiconfId = uiConfId;
        return this;
    }

    public KavaAnalyticsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public KavaAnalyticsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public KavaAnalyticsConfig setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public int getUiconfId() {
        return uiconfId;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getKs() {
        return ks;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
