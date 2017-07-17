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
 * @hide
 */

public class StatsService {
    private static final PKLog log = PKLog.get("StatsService");

    public static RequestBuilder sendStatsEvent(String baseUrl, int partnerId, int eventType, String clientVer, long duration,
                                                String sessionId, long position, int uiConfId, String entryId, String widgetId, boolean isSeek,
                                                int contextId, String applicationName, String userId) {
        return new RequestBuilder()
                .method("GET")
                .url(getOvpUrl(baseUrl, partnerId, eventType, clientVer, duration, sessionId, position, uiConfId, entryId, widgetId, isSeek,
                        contextId, applicationName, userId))
                .tag("stats-send");
    }

    private static String getOvpUrl(String baseUrl, int partnerId, int eventType, String clientVer, long duration,
                                    String sessionId, long position, int uiConfId, String entryId, String widgetId, boolean isSeek, int contextId,
                                    String applicationName, String userId) {
        Uri.Builder builder = new Uri.Builder();
        builder.path(baseUrl)
                .appendQueryParameter("service", "stats")
                .appendQueryParameter("apiVersion", "3.1")
                .appendQueryParameter("expiry", "86400")
                .appendQueryParameter("clientTag", "kwidget:v" + clientVer)
                .appendQueryParameter("format", "1")
                .appendQueryParameter("ignoreNull", "1")
                .appendQueryParameter("action", "collect")
                .appendQueryParameter("event:eventType", Integer.toString(eventType))
                .appendQueryParameter("event:clientVer", clientVer)
                .appendQueryParameter("event:currentPoint", Long.toString(position))
                .appendQueryParameter("event:duration", Long.toString(duration))
                .appendQueryParameter("event:eventTimeStamp", Long.toString(new Date().getTime()))
                .appendQueryParameter("event:isFirstInSession", "false")
                .appendQueryParameter("event:objectType", "KalturaStatsEvent")
                .appendQueryParameter("event:partnerId", Integer.toString(partnerId))
                .appendQueryParameter("event:sessionId", sessionId)
                .appendQueryParameter("event:uiconfId", Integer.toString(uiConfId))
                .appendQueryParameter("event:seek", Boolean.toString(isSeek))
                .appendQueryParameter("event:entryId", entryId)
                .appendQueryParameter("event:widgetId", widgetId);

        if (contextId > 0) {
            builder.appendQueryParameter("event:contextId", Integer.toString(contextId));
        }
        if (applicationName != null && !applicationName.isEmpty()) {
            builder.appendQueryParameter("event:applicationId", applicationName);
        }
        if (userId != null && !userId.isEmpty()) {
            builder.appendQueryParameter("event:userId", userId);
        }

        try {
            URL url =  new URL(URLDecoder.decode(builder.build().toString(), "UTF-8"));
            return url.toString();
        } catch (java.io.UnsupportedEncodingException ex) {
            log.d("UnsupportedEncodingException: ");
        } catch (MalformedURLException rx) {
            log.d("MalformedURLException: ");
        }
        return builder.build().toString();
    }
}
