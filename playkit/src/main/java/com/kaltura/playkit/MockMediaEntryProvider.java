package com.kaltura.playkit;

import com.kaltura.playkit.plugin.connect.OnCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MockMediaEntryProvider implements MediaEntryProvider {

    private PKMediaEntry mMediaEntry;
    private final JSONObject mJsonObject;

    public MockMediaEntryProvider(JSONObject jsonObject) throws JSONException {
        mJsonObject = jsonObject.getJSONObject("entries");
    }

    private PKDrmParams parseDrmData(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null) {
            return null;
        }

        PKDrmParams drmData = new PKDrmParams();
        drmData.licenseUri = jsonObject.getString("licenseUri");

        return drmData;
    }


    private PKMediaSource parseMediaSource(JSONObject jsonObject) throws JSONException {
        PKMediaSource mediaSource = new PKMediaSource();
        mediaSource.setId(jsonObject.getString("id"));
        mediaSource.setUrl(jsonObject.getString("url"));
        mediaSource.setMimeType(jsonObject.getString("mimeType"));

        JSONObject drmData = jsonObject.optJSONObject("drmData");
        mediaSource.setDrmData(parseDrmData(drmData));

        return mediaSource;
    }

    private PKMediaEntry parseMediaEntry(JSONObject jsonObject) throws JSONException {
        PKMediaEntry mediaEntry = new PKMediaEntry();
        mediaEntry.setId(jsonObject.getString("id"));
        mediaEntry.setDuration(jsonObject.getLong("duration"));

        JSONArray jsonArray = jsonObject.getJSONArray("sources");
        List<PKMediaSource> sources = new ArrayList<>(jsonArray.length());

        for (int i = 0, count=jsonArray.length(); i < count; i++) {
            JSONObject jsonSource = jsonArray.getJSONObject(i);
            sources.add(parseMediaSource(jsonSource));
        }

        mediaEntry.setSources(sources);

        return mediaEntry;
    }


    public void loadMediaEntry(String id) {
        try {
            JSONObject jsonEntry = mJsonObject.getJSONObject(id);

            mMediaEntry = parseMediaEntry(jsonEntry);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    public PKMediaEntry getMediaEntry() {
        return mMediaEntry;
    }

    @Override
    public void load(OnCompletion callback) {

    }
}
