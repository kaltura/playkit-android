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

package com.kaltura.playkit.api.ovp.services;

import android.net.Uri;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.playkit.PKLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

/**
 * Created by zivilan on 28/11/2016.
 */

public class AnalyticsService {
    private static final PKLog log = PKLog.get("AnalyticsService");

    public static RequestBuilder sendAnalyticsEvent(String baseUrl, int partnerId, int eventType, String clientVer, String playbackType, String sessionId, long position
                                                , int uiConfId, String entryId, int eventIdx, int flavourId, int bufferTime, int actualBitrate, String deliveryType) {
        return new RequestBuilder()
                .method("GET")
                .url(getAnalyticsUrl(baseUrl, partnerId, eventType, clientVer, playbackType, sessionId, position, uiConfId, entryId, eventIdx, flavourId, bufferTime,
                        actualBitrate, deliveryType))
                .tag("stats-send");
    }

    private static String getAnalyticsUrl(String baseUrl, int partnerId, int eventType, String clientVer, String playbackType, String sessionId, long position,
                                           int uiConfId, String entryId, int eventIdx, int flavourId, int bufferTime, int actualBitrate, String deliveryType) {
        Uri.Builder builder = new Uri.Builder();
        builder.path(baseUrl)
                .appendQueryParameter("service", "analytics")
                .appendQueryParameter("apiVersion", "3.1")
                .appendQueryParameter("expiry", "86400")
                .appendQueryParameter("clientTag", "kwidget:v" + clientVer)
                .appendQueryParameter("format", "1")
                .appendQueryParameter("ignoreNull", "1")
                .appendQueryParameter("action", "trackEvent")
                .appendQueryParameter("entryId", entryId)
                .appendQueryParameter("partnerId", Integer.toString(partnerId))
                .appendQueryParameter("eventType", Integer.toString(eventType))
                .appendQueryParameter("sessionId", sessionId)
                .appendQueryParameter("eventIndex", Integer.toString(eventIdx))
                .appendQueryParameter("bufferTime", Integer.toString(bufferTime))
                .appendQueryParameter("actualBitrate", Integer.toString(actualBitrate))
                .appendQueryParameter("flavourId", Integer.toString(flavourId))
                .appendQueryParameter("deliveryType", deliveryType)
                .appendQueryParameter("sessionStartTime", Long.toString(new Date().getTime()))
                .appendQueryParameter("uiconfId", Integer.toString(uiConfId))
                .appendQueryParameter("clientVer", clientVer)
                .appendQueryParameter("position", Long.toString(position))
                .appendQueryParameter("playbackType", playbackType);

        try {
            URL url = new URL(URLDecoder.decode(builder.build().toString(), "UTF-8"));
            return url.toString();
        } catch (java.io.UnsupportedEncodingException ex) {
            log.d("UnsupportedEncodingException: ");
        } catch (MalformedURLException rx) {
            log.d("MalformedURLException: ");
        }
        return builder.build().toString();
    }
}