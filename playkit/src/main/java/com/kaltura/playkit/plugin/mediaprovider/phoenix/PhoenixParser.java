package com.kaltura.playkit.plugin.mediaprovider.phoenix;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.Defines;
import com.kaltura.playkit.MediaEntry;
import com.kaltura.playkit.MediaSource;
import com.kaltura.playkit.plugin.mediaprovider.phoenix.data.AssetInfo;
import com.kaltura.playkit.plugin.mediaprovider.phoenix.data.MediaFile;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 03/11/2016.
 */

public class PhoenixParser {

    private MediaEntry parseMediaEntry(String assetInfoJson, String sourceFormat) {
        MediaEntry mediaEntry = new MediaEntry();

        JsonParser parser = new JsonParser();
        JsonObject assetJson = parser.parse(assetInfoJson).getAsJsonObject();
        mediaEntry.setId(assetJson.get("id").getAsString());

        ArrayList<MediaSource> sources = new ArrayList<>();
        JsonArray files = assetJson.getAsJsonArray("files");
        if(files != null && files.size() > 0){
            for(JsonElement fileElement : files){
                JsonObject file = fileElement.getAsJsonObject();
                if(sourceFormat == null || file.get("type").getAsString().equals(sourceFormat)){
                    MediaSource mediaSource = new MediaSource();
                    mediaSource.setId(file.get("id").getAsString());
                    mediaSource.setUrl(file.get("url").getAsString());
                    mediaSource.setMimeType(Defines.getMimeType(mediaSource.getUrl()));
                    //TODO: fetch drm data
                    sources.add(mediaSource);
                    break;
                }
            }
        }
        mediaEntry.setSources(sources);

        return mediaEntry;
    }

    public static MediaEntry getMedia(AssetInfo assetInfo) {
        MediaEntry mediaEntry = new MediaEntry();
        mediaEntry.setId(""+assetInfo.getId());

        ArrayList<MediaSource> sources = new ArrayList<>();
        for(MediaFile file : assetInfo.getFiles()){
            MediaSource source = new MediaSource();
            source.setId(""+file.getId());
            source.setUrl(file.getUrl());
            source.setMimeType(Defines.getMimeType(file.getUrl().));
            mediaEntry.setDuration(file.getDuration()); // ??
        }
    }
}
