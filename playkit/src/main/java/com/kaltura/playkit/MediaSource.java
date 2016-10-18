package com.kaltura.playkit;

import org.json.JSONException;
import org.json.JSONObject;

public class MediaSource {
    public final String id;
    public final String url;
    public final String mimeType;
    public final DRMData drmData;
    
    public MediaSource(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        url = jsonObject.getString("url");
        mimeType = jsonObject.getString("mimeType");
        JSONObject drmData = jsonObject.optJSONObject("drmData");
        this.drmData = drmData != null ? new DRMData(drmData) : null;
    }
}
