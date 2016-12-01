package com.kaltura.playkit.backend.ovp.services;

import android.net.Uri;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.connect.RequestBuilder;

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
                                                , int uiConfId, String entryId, int eventIdx, int flavourId, int bufferTime, int actualBitrate) {
        return new RequestBuilder()
                .method("GET")
                .url(getAnalyticsUrl(baseUrl, partnerId, eventType, clientVer, playbackType, sessionId, position, uiConfId, entryId, eventIdx, flavourId, bufferTime, actualBitrate))
                .tag("stats-send");
    }

    private static String getAnalyticsUrl(String baseUrl, int deliveryType, int eventType, String clientVer, String playbackType, String sessionId, long position,
                                           int uiConfId, String entryId, int eventIdx, int flavourId, int bufferTime, int actualBitrate) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(baseUrl)
                .path("/api_v3/index.php")
                .appendQueryParameter("service", "analytics")
                .appendQueryParameter("apiVersion", "3.1")
                .appendQueryParameter("expiry", "86400")
                .appendQueryParameter("clientTag", "kwidget:v" + clientVer)
                .appendQueryParameter("format", "1")
                .appendQueryParameter("ignoreNull", "1")
                .appendQueryParameter("action", "trackEvent")
                .appendQueryParameter("eventType", Integer.toString(eventType))
                .appendQueryParameter("position", Long.toString(position))
                .appendQueryParameter("deliveryType", Integer.toString(deliveryType))
                .appendQueryParameter("playbackType", playbackType)
                .appendQueryParameter("sessionStartTime", Long.toString(new Date().getTime()))
                .appendQueryParameter("eventIndex", Integer.toString(eventIdx))
                .appendQueryParameter("clientVer", clientVer)
                .appendQueryParameter("flavourId", Integer.toString(flavourId))
                .appendQueryParameter("sessionId", sessionId)
                .appendQueryParameter("uiconfId", Integer.toString(uiConfId))
                .appendQueryParameter("bufferTime", Integer.toString(bufferTime))
                .appendQueryParameter("entryId", entryId)
                .appendQueryParameter("actualBitrate", Integer.toString(actualBitrate));

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