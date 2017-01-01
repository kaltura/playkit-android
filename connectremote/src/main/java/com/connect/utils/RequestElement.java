package com.connect.utils;


import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Created by tehilarozin on 09/08/2016.
 */
public interface RequestElement {

    String getMethod();

    String getUrl();

    String getBody();

//    HashMap<String, String> getParams();

    String getTag();

    @NonNull Map<String, String> getHeaders();

    String getId();

    RequestConfiguration config();

    void onComplete(ResponseElement responseElement);
}
