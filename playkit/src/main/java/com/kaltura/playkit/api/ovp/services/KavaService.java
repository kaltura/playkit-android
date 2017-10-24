package com.kaltura.playkit.api.ovp.services;

import android.net.Uri;

import com.kaltura.netkit.connect.request.RequestBuilder;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by anton.afanasiev on 02/10/2017.
 */

public class KavaService {

    public static RequestBuilder sendAnalyticsEvent(String baseUrl, Map<String, String> params) {
        return new RequestBuilder()
                .method("GET")
                .url(buildUrlWithParams(baseUrl, params));
    }

    private static String buildUrlWithParams(String baserUrl, Map<String, String> params) {

        Uri.Builder builder = Uri.parse(baserUrl).buildUpon();

        Iterator iterator = params.entrySet().iterator();
        Map.Entry<String, String> entry;
        while (iterator.hasNext()) {
            entry = (Map.Entry) iterator.next();
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return builder.build().toString();
    }
}
