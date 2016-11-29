package com.kaltura.playkit.backend.ovp.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

import static com.kaltura.playkit.backend.phoenix.services.PhoenixService.getPhoenixParams;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class SessionService extends OvpService {

    public static RequestBuilder widgetSession(String baseUrl, int partnerId){
        return new RequestBuilder()
                .service("session")
                .action("startWidgetSession")
                .method("POST")
                .url(baseUrl)
                .tag("session-startWidget")
                .params(getWidgetSessionReqParams(partnerId));
    }

    public static JsonObject getWidgetSessionReqParams(int partnerId) {
        JsonObject params = getPhoenixParams();
        params.addProperty("widgetId", partnerId);
        return params;
    }

}
