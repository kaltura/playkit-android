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
        String taggedAction = request.tag() instanceof String ? (String) request.tag() : null;
        if(taggedAction != null){
            request = request.newBuilder().tag(UUID.randomUUID().toString()+"::"+taggedAction).build();
        }

        return chain.proceed(request);
    }
}
