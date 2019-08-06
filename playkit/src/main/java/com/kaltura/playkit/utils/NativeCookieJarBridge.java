package com.kaltura.playkit.utils;

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

// Bridge between OkHttp's CookieJar and HTTPURLConnection's CookieHandler
@SuppressWarnings({"ConstantConditions", "NullableProblems"})
public class NativeCookieJarBridge implements CookieJar {


    public static final NativeCookieJarBridge sharedCookieJar = new NativeCookieJarBridge();

    @Nullable
    private CookieStore explicitCookieStore;

    public NativeCookieJarBridge(@NonNull CookieStore explicitCookieStore) {
        this.explicitCookieStore = explicitCookieStore;
    }

    private NativeCookieJarBridge() {}

    private CookieStore cookieStore() {

        if (explicitCookieStore != null) {
            return explicitCookieStore;
        }

        final CookieHandler cookieHandler = CookieHandler.getDefault();

        return cookieHandler instanceof CookieManager ?
                ((CookieManager) cookieHandler).getCookieStore() :
                null;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

        //noinspection ConstantConditions
        if (url == null || cookies == null || cookies.isEmpty()) {
            return;
        }

        final CookieStore cookieStore = cookieStore();
        if (cookieStore == null) {
            return;
        }

        for (Cookie cookie : cookies) {

            final String value = cookie.value();
            final String name = cookie.name();
            final String domain = cookie.domain();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value) || TextUtils.isEmpty(domain)) {
                continue;
            }

            final HttpCookie hurlCookie = new HttpCookie(name, value);
            hurlCookie.setDomain(domain);

            final String path = cookie.path();
            if (path != null && path.startsWith("/")) {
                hurlCookie.setPath(path);
            }

            hurlCookie.setSecure(cookie.secure());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                hurlCookie.setHttpOnly(cookie.httpOnly());
            }

            cookieStore.add(url.uri(), hurlCookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {

        if (url == null) {
            return Collections.emptyList();
        }

        final CookieStore cookieStore = cookieStore();
        if (cookieStore == null) {
            return Collections.emptyList();
        }

        final List<HttpCookie> httpCookies = cookieStore.get(url.uri());
        if (httpCookies == null || httpCookies.isEmpty()) {
            return Collections.emptyList();
        }

        final ArrayList<Cookie> cookies = new ArrayList<>();
        for (HttpCookie httpCookie : httpCookies) {

            final String name = httpCookie.getName();
            final String value = httpCookie.getValue();
            final String domain = httpCookie.getDomain();
            final String path = httpCookie.getPath();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value) || TextUtils.isEmpty(domain)) {
                continue;
            }

            final Cookie.Builder builder = new Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(domain);

            if (path != null && path.startsWith("/")) {
                builder.path(path);
            }

            if (httpCookie.getSecure()) {
                builder.secure();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (httpCookie.isHttpOnly()) {
                    builder.httpOnly();
                }
            }

            cookies.add(builder.build());
        }
        return cookies;
    }
}
