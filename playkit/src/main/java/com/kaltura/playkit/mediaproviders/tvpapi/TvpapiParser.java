package com.kaltura.playkit.mediaproviders.tvpapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 03/11/2016.
 */

public class TvpapiParser {

    public static PKMediaEntry parseMediaEntry(String assetInfoJson, String sourceFormat) {
        PKMediaEntry mediaEntry = new PKMediaEntry();

        JsonParser parser = new JsonParser();
        JsonObject assetJson = parser.parse(assetInfoJson).getAsJsonObject();
        mediaEntry.setId(assetJson.get("id").getAsString());

        ArrayList<PKMediaSource> sources = new ArrayList<>();
        JsonArray files = assetJson.getAsJsonArray("files");
        if(files != null && files.size() > 0){
            for(JsonElement fileElement : files){
                JsonObject file = fileElement.getAsJsonObject();
                if(sourceFormat == null || file.get("type").getAsString().equals(sourceFormat)){
                    PKMediaSource mediaSource = new PKMediaSource();
                    mediaSource.setId(file.get("id").getAsString());
                    mediaSource.setUrl(file.get("url").getAsString());
                    //TODO: fetch drm data
                    sources.add(mediaSource);
                    break;
                }
            }
        }
        mediaEntry.setSources(sources);

        return mediaEntry;
    }

}
