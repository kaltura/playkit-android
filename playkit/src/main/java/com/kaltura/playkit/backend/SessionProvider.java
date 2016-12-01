package com.kaltura.playkit.backend;

/**
 * Created by tehilarozin on 13/11/2016.
 */

/**
 * provides session related configuration data to be used by who ever needs to communicate with
 * a remote data provider
 */
public interface SessionProvider {

    String baseUrl();

    String getKs();

    int partnerId();

    //onSessionExpired
}
