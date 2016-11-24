package com.kaltura.playkit.plugins.ads;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.PKMediaFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class AdsConfig {
    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String ENABLE_BG_PLAYBACK  = "enableBackgroundPlayback";
    public static final String AUTO_PLAY_AD_BREAK  = "autoPlayAdBreaks";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String VIDEO_MIME_TYPES    = "videoMimeTypes";
    public static final String AD_TAG_TIMES        = "tagsTimes";

    private String language = "en";
    private String adTagUrl;
    private boolean enableBackgroundPlayback = true;
    private boolean autoPlayAdBreaks = false;
    private int videoBitrate;
    private List<String> videoMimeTypes;
    private Map<Double,String> tagsTimes; // <AdTime,URL_to_execute>
    //View companionView;
    //ViewGroup uiControlsContainer;

    public AdsConfig(String language, boolean enableBackgroundPlayback, boolean autoPlayAdBreaks, int videoBitrate, List<String> videoMimeTypes, String adTagUrl, Map<Double,String> tagTimes) {//}, String tagsTimes, View companionView, ViewGroup uiControlsContainer) {
        this.language = language;
        this.enableBackgroundPlayback = enableBackgroundPlayback;
        this.autoPlayAdBreaks = autoPlayAdBreaks;
        this.videoBitrate = videoBitrate;
        if (videoMimeTypes == null) {
            videoMimeTypes = new ArrayList<>();
        }
        this.videoMimeTypes = videoMimeTypes;
        this.adTagUrl = adTagUrl;
        if (tagTimes == null) {
            tagTimes = new HashMap<>();
        }
        this.tagsTimes = tagTimes;
        //this.companionView = companionView;
        //this.uiControlsContainer = uiControlsContainer;
    }


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean getEnableBackgroundPlayback() {
        return enableBackgroundPlayback;
    }

    public void setEnableBackgroundPlayback(boolean enableBackgroundPlayback) {
        this.enableBackgroundPlayback = enableBackgroundPlayback;
    }

    public boolean getAutoPlayAdBreaks() {
        return autoPlayAdBreaks;
    }

    public void setAutoPlayAdBreaks(boolean autoPlayAdBreaks) {
        this.autoPlayAdBreaks = autoPlayAdBreaks;
    }

    public long getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public List<String> getVideoMimeTypes() {
        return videoMimeTypes;
    }

    public void setVideoMimeTypes(List<String> videoMimeTypes) {
        this.videoMimeTypes = videoMimeTypes;
    }

    public String getAdTagUrl() {
        return adTagUrl;
    }

    public void setAdTagUrl(String adTagUrl) {
        this.adTagUrl = adTagUrl;
    }

    public Map<Double, String> getTagsTimes() {
        return tagsTimes;
    }

    public void setTagsTimes(Map<Double, String> tagsTimes) {
        this.tagsTimes = tagsTimes;
    }

//    public View getCompanionView() {
//        return companionView;
//    }
//
//    public void setCompanionView(View companionView) {
//        this.companionView = companionView;
//    }
//
//    public ViewGroup getUiControlsContainer() {
//        return uiControlsContainer;
//    }
//
//    public void setUiControlsContainer(ViewGroup uiControlsContainer) {
//        this.uiControlsContainer = uiControlsContainer;
//    }

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(AD_TAG_URL, adTagUrl);
        jsonObject.addProperty(ENABLE_BG_PLAYBACK, enableBackgroundPlayback);
        jsonObject.addProperty(AUTO_PLAY_AD_BREAK, autoPlayAdBreaks);
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);

        Gson gson = new Gson();
        JsonArray jArray = new JsonArray();
        for (String mimeType : videoMimeTypes) {
            JsonPrimitive element = new JsonPrimitive(mimeType);
            jArray.add(element);
        }
        jsonObject.add(VIDEO_MIME_TYPES, jArray);

        String tagsTimesJsonString = gson.toJson(tagsTimes);
        if (tagsTimesJsonString != null && !tagsTimesJsonString.isEmpty()) {
            JsonParser parser = new JsonParser();
            JsonObject tagsTimesJsonObject = parser.parse(tagsTimesJsonString).getAsJsonObject();
            jsonObject.add(AD_TAG_TIMES, tagsTimesJsonObject);
        } else {
            jsonObject.add(AD_TAG_TIMES, new JsonObject());
        }

        return jsonObject;
    }

    public static AdsConfig fromJsonObject(JsonObject adsConfigJson) {
        Gson gson = new Gson();
        String language = (adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_TAG_LANGUAGE) != null) ? adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_TAG_LANGUAGE).getAsString() : "en";
        String adTagUrl = (adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_TAG_URL) != null) ? adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_TAG_URL).getAsString() : "";
        boolean enableBackgroundPlayback = (adsConfigJson.getAsJsonPrimitive(AdsConfig.ENABLE_BG_PLAYBACK) != null) ? adsConfigJson.getAsJsonPrimitive(AdsConfig.ENABLE_BG_PLAYBACK).getAsBoolean() : false;
        boolean autoPlayAdBreaks = (adsConfigJson.getAsJsonPrimitive(AdsConfig.AUTO_PLAY_AD_BREAK) != null) ? adsConfigJson.getAsJsonPrimitive(AdsConfig.AUTO_PLAY_AD_BREAK).getAsBoolean() : true;
        int videoBitrate = (adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_VIDEO_BITRATE) != null) ? adsConfigJson.getAsJsonPrimitive(AdsConfig.AD_VIDEO_BITRATE).getAsInt() : -1;
        JsonArray mimeTypesJsonArray = adsConfigJson.getAsJsonArray(AdsConfig.VIDEO_MIME_TYPES);
        List<String> videoMimeTypes = new ArrayList<>();

        if (mimeTypesJsonArray != null && mimeTypesJsonArray.size() > 0) {
            Iterator<JsonElement> iterator = mimeTypesJsonArray.iterator();
            while (iterator.hasNext()) {
                JsonPrimitive jsonElement = (JsonPrimitive) iterator.next();
                String mimeType = gson.fromJson(jsonElement, String.class);
                videoMimeTypes.add(mimeType);
            }
        } else {
            videoMimeTypes.add(PKMediaFormat.mp4_clear.mimeType);
        }

        Map<Double,String> tagTimes = new HashMap<>();
 //       Type type = new TypeToken<Map<Double, String>>(){}.getType();
 //       tagTimes = gson.fromJson(adsConfigJson.getAsJsonObject(AdsConfig.AD_TAG_TIMES).getAsJsonObject(), type);
        return new AdsConfig(language, enableBackgroundPlayback, autoPlayAdBreaks, videoBitrate, videoMimeTypes, adTagUrl, tagTimes);
    }
}

