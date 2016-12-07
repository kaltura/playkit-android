package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.connect.ErrorElement;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public abstract class BaseSessionProvider implements SessionProvider{


    public interface SessionProviderListener{
        void onError(ErrorElement error);
        void ready();
    }

    protected String baseUrl;
    protected int partnerId;
    private String sessionToken;
    protected long expiryDate;

    protected SessionProviderListener sessionListener;


    protected BaseSessionProvider(String baseUrl, int partnerId){
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
    }


    public void setSessionProviderListener(SessionProviderListener listener){
        this.sessionListener = listener;
    }

    public void endSession(){
        clearSession();
    }

    protected void clearSession() {
        sessionToken = null;
        expiryDate = 0;
    }


    @Override
    public int partnerId() {
        return partnerId;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    protected void setSession(String sessionToken, long expiry){
        this.sessionToken = sessionToken;
        this.expiryDate = expiry;
    }

    protected String getSessionToken() {
        return sessionToken;
    }

    protected boolean isSessionActive(){
        return sessionToken != null;
    }

    protected abstract String validateSession();
}
