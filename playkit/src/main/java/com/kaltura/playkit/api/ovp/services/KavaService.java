package com.kaltura.playkit.api.ovp.services;

import android.net.Uri;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.playkit.PKLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by anton.afanasiev on 02/10/2017.
 */

public class KavaService {

    private static final PKLog log = PKLog.get(KavaService.class.getSimpleName());

    public static RequestBuilder sendAnalyticsEvent(String baseUrl, Map<String, String> params) {
        return new RequestBuilder()
                .method("GET")
                .url(buildUrlWithParams(baseUrl, params));
    }

    private static String buildUrlWithParams(String baserUrl, Map<String, String> params) {
        Uri.Builder builder = new Uri.Builder();

        builder.path(baserUrl);

        Iterator iterator = params.entrySet().iterator();
        Map.Entry<String, String> entry;
        while (iterator.hasNext()) {
            entry = (Map.Entry) iterator.next();
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
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
