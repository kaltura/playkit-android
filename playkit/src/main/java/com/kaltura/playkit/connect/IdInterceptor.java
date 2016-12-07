package com.kaltura.playkit.connect;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by tehilarozin on 01/09/2016.
 */
public class IdInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        // if tag was not supplied to the request, the default tag is the request itself
        String taggedAction = request.tag().equals(request) ? null : (String) request.tag();

        String idTag = UUID.randomUUID().toString() + "::" + (taggedAction != null ? taggedAction : request.url().toString());
        request = request.newBuilder().tag(idTag).build();

        return chain.proceed(request);
    }
}
