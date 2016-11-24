package com.kaltura.playkit.backend.ovp.services;

import android.net.Uri;

import com.kaltura.playkit.connect.RequestBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created by zivilan on 24/11/2016.
 */

public class LiveStatsService {
    public static RequestBuilder sendLiveStatsEvent(String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
                                                String sessionId, String startTime,  String entryId,  boolean isLive, String referrer) {
        return new RequestBuilder()
                .method("GET")
                .url(getOvpUrl(baseUrl, partnerId, eventType, eventIndex, bufferTime, bitrate, sessionId, startTime,  entryId,  isLive, referrer))
                .tag("stats-send");
    }

    private static String getOvpUrl(String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
                                    String sessionId, String startTime,  String entryId,  boolean isLive, String referrer) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(baseUrl)
                .path("/api_v3/index.php")
                .appendQueryParameter("service", "stats")
                .appendQueryParameter("apiVersion", "3.1")
                .appendQueryParameter("expiry", "86400")
                .appendQueryParameter("format", "1")
                .appendQueryParameter("ignoreNull", "1")
                .appendQueryParameter("action", "collect")
                .appendQueryParameter("event:eventType", Integer.toString(eventType))
                .appendQueryParameter("event:sessionId", sessionId)
                .appendQueryParameter("event:eventIndex", Integer.toString(eventIndex))
                .appendQueryParameter("event:bufferTime", Integer.toString(bufferTime))
                .appendQueryParameter("event:bitrate", Integer.toString(bitrate))
                .appendQueryParameter("event:referrer", referrer)
                .appendQueryParameter("event:isLive", Boolean.toString(isLive))
                .appendQueryParameter("event:startTime", startTime)
                .appendQueryParameter("event:entryId", entryId)
                .appendQueryParameter("event:deliveryType", "");

        try {
            URL url = new URL(URLDecoder.decode(builder.build().toString(), "UTF-8"));
            return url.toString();
        } catch (java.io.UnsupportedEncodingException ex) {

        } catch (MalformedURLException rx) {

        }
        return builder.build().toString();
    }
}
