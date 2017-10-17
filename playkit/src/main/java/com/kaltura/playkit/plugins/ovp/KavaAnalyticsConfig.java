package com.kaltura.playkit.plugins.ovp;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils;

/**
 * Created by anton.afanasiev on 04/10/2017.
 */

public class KavaAnalyticsConfig {

    private static final PKLog log = PKLog.get(KavaAnalyticsConfig.class.getSimpleName());

    private static final String DEFAULT_BASE_URL = "http://analytics.kaltura.com/api_v3/index.php";

    private static final String KS_KEY = "ks";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String UI_CONF_ID_KEY = "uiconfId";
    private static final String PARTNER_ID_KEY = "partnerId";
    private static final String CUSTOM_VAR_1_KEY = "customVar1";
    private static final String CUSTOM_VAR_2_KEY = "customVar2";
    private static final String CUSTOM_VAR_3_KEY = "customVar3";
    private static final String PLAYBACK_CONTEXT_KEY = "playbackContext";
    private static final String REFERRER_KEY = "referrer";


    private int uiconfId = 0;
    private int partnerId = 0;

    private String ks;
    private String baseUrl = DEFAULT_BASE_URL;
    private String customVar1, customVar2, customVar3;
    private String playbackContext;
    private String referrerAsBase64;

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
        }

        if(Utils.isJsonObjectValueValid(config, CUSTOM_VAR_1_KEY)) {
            customVar1 = config.getAsJsonPrimitive(CUSTOM_VAR_1_KEY).getAsString();
        }

        if(Utils.isJsonObjectValueValid(config, CUSTOM_VAR_2_KEY)) {
            customVar2 = config.getAsJsonPrimitive(CUSTOM_VAR_2_KEY).getAsString();
        }

        if(Utils.isJsonObjectValueValid(config, CUSTOM_VAR_3_KEY)) {
            customVar3 = config.getAsJsonPrimitive(CUSTOM_VAR_3_KEY).getAsString();
        }
        
        if(Utils.isJsonObjectValueValid(config, PLAYBACK_CONTEXT_KEY)) {
            playbackContext = config.getAsJsonPrimitive(PLAYBACK_CONTEXT_KEY).getAsString();
        }

        if(Utils.isJsonObjectValueValid(config, REFERRER_KEY)) {
            String referrer = config.getAsJsonPrimitive(REFERRER_KEY).getAsString();
            if(isValidReferrer(referrer)){
                this.referrerAsBase64 = Utils.toBase64(referrer.getBytes());
            } else {
                this.referrerAsBase64 = null;
            }
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

    public KavaAnalyticsConfig setCustomVar1(String customVar1) {
        this.customVar1 = customVar1;
        return this;
    }

    public KavaAnalyticsConfig setCustomVar2(String customVar2) {
        this.customVar2 = customVar2;
        return this;
    }

    public KavaAnalyticsConfig setCustomVar3(String customVar3) {
        this.customVar3 = customVar3;
        return this;
    }

    public KavaAnalyticsConfig setReferrer(String referrer) {
        if(isValidReferrer(referrer)) {
            this.referrerAsBase64 = Utils.toBase64(referrer.getBytes());
        } else {
            log.w("Invalid referrer argument. Should start with app:// or http:// or https://");
            referrerAsBase64 = null;
        }

        return this;

    }

    public KavaAnalyticsConfig setPlaybackContext(String playbackContext) {
        this.playbackContext = playbackContext;
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

    public String getCustomVar1() {
        return customVar1;
    }

    public String getCustomVar2() {
        return customVar2;
    }

    public String getCustomVar3() {
        return customVar3;
    }

    public String getPlaybackContext() {
        return playbackContext;
    }

    public String getReferrerAsBase64() {
        return referrerAsBase64;
    }

    private boolean isValidReferrer(String referrer) {
        return (referrer.startsWith("app://") || referrer.startsWith("http://") || referrer.startsWith("https://"));
    }
}
