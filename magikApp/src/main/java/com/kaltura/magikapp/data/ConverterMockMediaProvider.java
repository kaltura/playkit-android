package com.kaltura.magikapp.data;

import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * Created by itanbarpeled on 11/12/2016.
 */

public class ConverterMockMediaProvider extends ConverterMediaProvider {

    String mediaId;
    HashMap<String, JsonObject> entries;


    public HashMap<String, JsonObject> getEntries() {
        return entries;
    }


    public String getMediaId() {
        return mediaId;
    }


}
