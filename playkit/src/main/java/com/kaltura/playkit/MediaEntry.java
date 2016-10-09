package com.kaltura.playkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MediaEntry {
    public final String id;
    public final List<MediaSource> sources;
    public final long duration;
    
    public MediaEntry(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        duration = jsonObject.getLong("duration");
        
        JSONArray jsonArray = jsonObject.getJSONArray("sources");
        sources = new ArrayList<>(jsonArray.length());
        
        for (int i = 0, count=jsonArray.length(); i < count; i++) {
            JSONObject jsonSource = jsonArray.getJSONObject(i);
            sources.add(new MediaSource(jsonSource));
        }
    }
}
