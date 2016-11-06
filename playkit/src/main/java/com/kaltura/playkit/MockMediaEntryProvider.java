package com.kaltura.playkit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class MockMediaEntryProvider implements MediaEntryProvider {

    private Entries mEntries;
    private MediaEntry mMediaEntry;

    /*public MockMediaEntryProvider(String id) throws JSONException {
        mMediaEntry = new MediaEntry(new JSONObject());
    }*/

    public MockMediaEntryProvider(String fileName, int id) throws FileNotFoundException {
        this(fileName);
        mMediaEntry = mEntries.get(id);
    }

    public MockMediaEntryProvider(String fileName, String id) throws FileNotFoundException {
        this(fileName);
        mMediaEntry = mEntries.get(id);
    }

    public MockMediaEntryProvider(String fileName) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(new FileReader(fileName));
        mEntries = new Gson().fromJson(element, Entries.class);


        /*mEntries = element.getAsJsonObject().getAsJsonArray("entries");

        for(JsonElement jsonElement : mEntries){
            JsonObject object = jsonElement.getAsJsonObject();
            if(object.get("id").getAsString().equals(id)){
                mMediaEntry =
            }
        }*/
    }
    
    @Override
    public MediaEntry getMediaEntry() {
        return mMediaEntry;
    }
}
