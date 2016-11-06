package com.kaltura.playkit.plugin.mediaprovider.mock;

import com.google.gson.Gson;
import com.kaltura.playkit.MediaEntry;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaParser {

    public static MediaEntry parseMedia(String mediaJson){
        /*JsonParser parser = new JsonParser();
        parser.parse(mediaJson);*/

        MediaEntry mediaEntry = new Gson().fromJson(mediaJson, MediaEntry.class);
        return mediaEntry;
    }
}
