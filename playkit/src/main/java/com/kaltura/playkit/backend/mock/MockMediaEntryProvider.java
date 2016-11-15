package com.kaltura.playkit.backend.mock;

import android.content.Context;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.kaltura.playkit.Utils.fullyReadInputStream;

public class MockMediaEntryProvider implements MediaEntryProvider {

    private PKMediaEntry mediaEntry;
    private JSONObject jsonObject;

    public MockMediaEntryProvider() {}

    public MockMediaEntryProvider(JSONObject jsonObject) throws JSONException {
        this.jsonObject = jsonObject.getJSONObject("entries");
    }

    public MockMediaEntryProvider setJSONInputFile(String filename) throws IOException, JSONException {
        InputStream inputStream = new FileInputStream(filename);
        String jsonString = fullyReadInputStream(inputStream, 1024 * 1024).toString();
        inputStream.close();
        jsonObject = new JSONObject(jsonString).getJSONObject("entries");
        return this;
    }

    public MockMediaEntryProvider setInputJSONAsset(Context context, String assetId) throws IOException, JSONException {
        String jsonString = Utils.readAssetToString(context, assetId);
        jsonObject = new JSONObject(jsonString).getJSONObject("entries");
        return this;
    }

    private PKDrmParams parseDrmData(JSONObject jsonObject) throws JSONException {
        
        if (jsonObject == null) {
            return null;
        }

        return new PKDrmParams(jsonObject.getString("licenseUri"));
    }

    private PKMediaSource parseMediaSource(JSONObject jsonObject) throws JSONException {
        PKMediaSource mediaSource = new PKMediaSource();
        mediaSource.setId(jsonObject.getString("id"));
        mediaSource.setUrl(jsonObject.getString("url"));

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


    public MockMediaEntryProvider setMediaId(String id) {
        try {
            JSONObject jsonEntry = jsonObject.getJSONObject(id);

            mediaEntry = parseMediaEntry(jsonEntry);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }
    
    @Override
    public void load(OnMediaLoadCompletion callback) {

    }
}
