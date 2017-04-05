package com.kaltura.playkit;

import android.net.Uri;

import java.util.Map;

public class PKRequestInfo {

    private Uri url;
    private Map<String, String> headers;

    public PKRequestInfo(Uri url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public Uri getUrl() {
        return url;
    }

    public PKRequestInfo setUrl(Uri url) {
        this.url = url;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public PKRequestInfo setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public interface Decorator {
        PKRequestInfo getRequestInfo(PKRequestInfo requestInfo);
    }
}
