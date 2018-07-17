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

package providers.playkit.kaltura.com.mediaproviders.api.ovp.services;

import android.net.Uri;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.playkit.PKLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * @hide
 */

public class LiveStatsService {
    private static final PKLog log = PKLog.get("LiveStatsService");

    public static RequestBuilder sendLiveStatsEvent(String baseUrl, int partnerId, int eventType, int eventIndex, long bufferTime, long bitrate,
                                                    String sessionId, long startTime, String entryId, boolean isLive, String clientVer, String deliveryType) {
        return new RequestBuilder()
                .method("GET")
                .url(getOvpUrl(baseUrl, partnerId, eventType, eventIndex, bufferTime, bitrate, sessionId, startTime, entryId, isLive, clientVer, deliveryType))
                .tag("stats-send");
    }

    private static String getOvpUrl(String baseUrl, int partnerId, int eventType, int eventIndex, long bufferTime, long bitrate,
                                    String sessionId, long startTime, String entryId, boolean isLive, String clientVer, String deliveryType) {
        Uri.Builder builder = new Uri.Builder();
        builder.path(baseUrl)
                .appendQueryParameter("service", "liveStats")
                .appendQueryParameter("apiVersion", "3.1")
                .appendQueryParameter("expiry", "86400")
                .appendQueryParameter("format", "1")
                .appendQueryParameter("ignoreNull", "1")
                .appendQueryParameter("action", "collect")
                .appendQueryParameter("clientTag", "kwidget:v" + clientVer)
                .appendQueryParameter("event:eventType", Integer.toString(eventType))
                .appendQueryParameter("event:partnerId", Integer.toString(partnerId))
                .appendQueryParameter("event:sessionId", sessionId)
                .appendQueryParameter("event:eventIndex", Integer.toString(eventIndex))
                .appendQueryParameter("event:bufferTime", Long.toString(bufferTime))
                .appendQueryParameter("event:bitrate", Long.toString(bitrate))
                .appendQueryParameter("event:isLive", Boolean.toString(isLive))
                .appendQueryParameter("event:startTime", Long.toString(startTime))
                .appendQueryParameter("event:entryId", entryId)
                .appendQueryParameter("event:deliveryType", deliveryType);

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
