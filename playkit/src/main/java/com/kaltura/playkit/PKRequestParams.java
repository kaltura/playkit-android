package com.kaltura.playkit;

import android.net.Uri;

import java.util.Map;

public class PKRequestParams {

    public final Uri url;
    public final Map<String, String> headers;

    public PKRequestParams(Uri url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public interface Adapter {
        PKRequestParams adapt(PKRequestParams requestParams);
    }
}
