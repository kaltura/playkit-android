package com.connect.utils;

import java.util.UUID;

/**
 * Created by tehilarozin on 01/12/2016.
 */
public class RequestIdFactory implements APIOkRequestsExecutor.IdFactory {
    @Override
    public String factorId(String factor) {
        return UUID.randomUUID().toString() + "::" + (factor!=null ? factor : System.currentTimeMillis());
    }
}
