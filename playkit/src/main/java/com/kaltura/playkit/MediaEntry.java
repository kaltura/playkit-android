package com.kaltura.playkit;

import java.util.List;

public class MediaEntry {

    private String id;
    private List<MediaSource> sources;
    private long duration;
    
    /*public MediaEntry(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        duration = jsonObject.getLong("duration");
        
        JSONArray jsonArray = jsonObject.getJSONArray("sources");
        sources = new ArrayList<>(jsonArray.length());
        
        for (int i = 0, count=jsonArray.length(); i < count; i++) {
            JSONObject jsonSource = jsonArray.getJSONObject(i);
            sources.add(new MediaSource(jsonSource));
        }
    }*/

    public String getId() {
        return id;
    }

    public List<MediaSource> getSources() {
        return sources;
    }

    public long getDuration() {
        return duration;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSources(List<MediaSource> sources) {
        this.sources = sources;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
