package com.kaltura.playkit;

public class MediaSource {
    private String id;
    private String url;
    private String mimeType;
    private DRMData drmData;
    
    /*public MediaSource(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString("id");
        url = jsonObject.getString("url");
        mimeType = jsonObject.getString("mimeType");
        JSONObject drmData = jsonObject.optJSONObject("drmData");
        this.drmData = drmData != null ? new DRMData(drmData) : null;
    }*/

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public DRMData getDrmData() {
        return drmData;
    }

    public void setDrmData(DRMData drmData) {
        this.drmData = drmData;
    }
}
