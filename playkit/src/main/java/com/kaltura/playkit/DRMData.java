package com.kaltura.playkit;

import org.json.JSONException;
import org.json.JSONObject;

public class DRMData {
    public final String licenseUri;

    public DRMData(JSONObject drmData) throws JSONException {
        licenseUri = drmData.getString("licenseUri");
    }
}
