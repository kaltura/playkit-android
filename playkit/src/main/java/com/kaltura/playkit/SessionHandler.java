package com.kaltura.playkit;

import android.support.annotation.StringDef;

import com.connect.backend.SessionProvider;
import com.connect.backend.base.BaseSessionProvider;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.kaltura.playkit.SessionHandler.SessionType.OVP;
import static com.kaltura.playkit.SessionHandler.SessionType.OttPhoenix;
import static com.kaltura.playkit.SessionHandler.SessionType.OttTvpapi;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 11/12/2016.
 */

public class SessionHandler {

    private String playSessionId;

    @Retention(SOURCE)
    @StringDef(value = {OVP, OttPhoenix, OttTvpapi})
    public @interface SessionType {
        String OVP = "ovp";
        String OttPhoenix = "ott_phoenix";
        String OttTvpapi = "ott_tvpapi";
    }

    private Map<String, SessionProvider> sessionProviders = new HashMap<>();

    public String getPlaySessionId(){
        if(playSessionId == null){
            playSessionId = UUID.randomUUID().toString();
        }
        return playSessionId;
    }

    public synchronized SessionHandler setSession(@SessionType String type, SessionProvider session){
        sessionProviders.put(type, session);
        return this;
    }

    public synchronized <T extends BaseSessionProvider> T getSession(@SessionType String type){
        return (T) sessionProviders.get(type);
    }
}
