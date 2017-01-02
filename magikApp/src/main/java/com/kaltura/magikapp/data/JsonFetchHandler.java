package com.kaltura.magikapp.data;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static com.kaltura.magikapp.data.JsonFetchHandler.JsonType.STANDALONE_PLAYER;


/**
 * Created by itanbarpeled on 05/12/2016.
 */

public class JsonFetchHandler {


    private static final String LOCAL_STANDALONE_JSON_FILE_NAME = "standalonePlayer.json";
    private static final String LOCAL_PLAY_KIT_APP_JSON_FILE_NAME = "playKitApp.json";
    private static final String LOCAL_JSON_SCHEME = "file";


    public enum JsonType {
        PLAY_KIT_APP,
        STANDALONE_PLAYER
    }


    /**
     * This method fetch the json representing a standalonePlayer or playKitApp.
     * @param jsonUrl - mUrl pointing to the remote json. if null this means we should use the local json.
     * @param jsonType
     * @param context
     * @param onJsonFetchedListener
     */
    public static void fetchJson(String jsonUrl, JsonType jsonType, Context context, OnJsonFetchedListener onJsonFetchedListener) {

        if (!TextUtils.isEmpty(jsonUrl)) { // app was invoked via deep linking

            fetchRemoteJson(jsonUrl, jsonType, context, onJsonFetchedListener);

        } else { // app invocation was done by clicking the app icon from device home screen

            fetchLocalJson(jsonType, context, onJsonFetchedListener);
        }

    }



    public static void fetchPlayerConfig(String playerConfigUri, Context context, OnJsonFetchedListener onJsonFetchedListener) {


        URL url = null;

        try {

            url = new URL(playerConfigUri);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            onJsonFetchedListener.onJsonFetched(null);
            return;
        }

        if (url.getProtocol().equals(LOCAL_JSON_SCHEME)) {

            String localFilePath = url.getHost() + url.getFile();
            fetchLocalJson(STANDALONE_PLAYER, localFilePath, context, onJsonFetchedListener);

        } else {

            fetchRemoteJson(playerConfigUri, STANDALONE_PLAYER, false, context, onJsonFetchedListener);
        }


    }


    private static void fetchLocalJson(JsonType jsonType, Context context, OnJsonFetchedListener onJsonFetchedListener) {

        String localPath = (jsonType == STANDALONE_PLAYER ? LOCAL_STANDALONE_JSON_FILE_NAME : LOCAL_PLAY_KIT_APP_JSON_FILE_NAME);

        fetchLocalJson(jsonType, localPath, context, onJsonFetchedListener);

    }


    private static void fetchLocalJson(JsonType jsonType, String localPath, Context context, OnJsonFetchedListener onJsonFetchedListener) {

        String json = null;
        InputStream inputStream = null;

        try {

            inputStream = context.getAssets().open(localPath);

            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {

            ex.printStackTrace();

        } finally {

            try {

                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            onJsonFetchedListener.onJsonFetched(json);

        }

    }

    private static void fetchRemoteJson(String playerConfigUrl, JsonType jsonType, Context context, final OnJsonFetchedListener onJsonFetchedListener) {

        boolean showSpinner = (jsonType == STANDALONE_PLAYER);

        fetchRemoteJson(playerConfigUrl, jsonType, showSpinner, context, onJsonFetchedListener);

    }



    private static void fetchRemoteJson(String playerConfigUrl, JsonType jsonType, boolean showSpinner, Context context, final OnJsonFetchedListener onJsonFetchedListener) {

        new JsonFetchAsyncTask(context, showSpinner, new OnJsonFetchedListener() {

            @Override
            public void onJsonFetched(String json) {

                onJsonFetchedListener.onJsonFetched(json);

            }


        }).execute(playerConfigUrl);

    }



    public interface OnJsonFetchedListener {
        void onJsonFetched(String json);
    }




}
