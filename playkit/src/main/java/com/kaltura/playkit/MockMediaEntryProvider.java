package com.kaltura.playkit;

import org.json.JSONException;
import org.json.JSONObject;

public class MockMediaEntryProvider implements MediaEntryProvider {

    private final MediaEntry mMediaEntry;

    public MockMediaEntryProvider(String id) throws JSONException {
        mMediaEntry = new MediaEntry(new JSONObject());
    }
    
    @Override
    public MediaEntry getMediaEntry() {
        return mMediaEntry;
    }
}
