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

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlaykitUtils;

/**
 * Created by anton.afanasiev on 04/10/2017.
 */

public class KavaAnalyticsConfig {

    private static final PKLog log = PKLog.get(KavaAnalyticsConfig.class.getSimpleName());

    private static final String DEFAULT_BASE_URL = "http://analytics.kaltura.com/api_v3/index.php";

    private int uiconfId;
    private int partnerId;

    private String ks;
    private String playbackContext;
    private String referrerAsBase64;
    private String baseUrl = DEFAULT_BASE_URL;
    private String customVar1, customVar2, customVar3;

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
        if (isValidReferrer(referrer)) {
            this.referrerAsBase64 = PlaykitUtils.toBase64(referrer.getBytes());
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

    int getUiConfId() {
        return uiconfId;
    }

    int getPartnerId() {
        return partnerId;
    }

    String getKs() {
        return ks;
    }

    String getBaseUrl() {
        return baseUrl;
    }

    String getCustomVar1() {
        return customVar1;
    }

    String getCustomVar2() {
        return customVar2;
    }

    String getCustomVar3() {
        return customVar3;
    }

    String getPlaybackContext() {
        return playbackContext;
    }

    String getReferrerAsBase64() {
        return referrerAsBase64;
    }

    boolean hasPlaybackContext() {
        return playbackContext != null;
    }

    boolean hasCustomVar1() {
        return customVar1 != null;
    }

    boolean hasCustomVar2() {
        return customVar2 != null;
    }

    boolean hasCustomVar3() {
        return customVar3 != null;
    }

    boolean hasKs() {
        return ks != null;
    }

    boolean hasUiConfId() {
        return uiconfId != 0;
    }

    private boolean isValidReferrer(String referrer) {
        return (referrer.startsWith("app://") || referrer.startsWith("http://") || referrer.startsWith("https://"));
    }

    boolean isPartnerIdValid() {
        return partnerId != 0;
    }
}
