package com.kaltura.playkit.plugins.ads.ima;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAConfig {
    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String ENABLE_BG_PLAYBACK  = "enableBackgroundPlayback";
    public static final String AUTO_PLAY_AD_BREAK  = "autoPlayAdBreaks";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String AD_VIDEO_MIME_TYPES    = "videoMimeTypes";
    public static final String AD_TAG_TIMES        = "tagsTimes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT  = "adCountDown";
    public static final String AD_PLAY_ON_RESUME  = "playOnResume";


    private String language = "en";
    private String adTagURL;
    private boolean enableBackgroundPlayback = true;
    private boolean autoPlayAdBreaks = false;
    private int videoBitrate;
    private boolean adAttribution;
    private boolean adCountDown;
    private boolean playOnAdResume;
    private List<String> videoMimeTypes;
    //private Map<Double,String> tagsTimes; // <AdTime,URL_to_execute>

    //View companionView;

    public IMAConfig(String language, boolean enableBackgroundPlayback, boolean autoPlayAdBreaks, int videoBitrate,
                     List<String> videoMimeTypes, String adTagUrl, boolean adAttribution, boolean adCountDown, boolean playOnAdResume) {
        this.enableBackgroundPlayback = enableBackgroundPlayback;
        this.autoPlayAdBreaks = autoPlayAdBreaks;
        this.videoBitrate = videoBitrate;
        this.adTagURL = adTagUrl;
        this.adAttribution = adAttribution;
        this.adCountDown = adCountDown;
        this.playOnAdResume = playOnAdResume;

        if (videoMimeTypes == null) {
            videoMimeTypes = new ArrayList<>();
        }
        this.videoMimeTypes = videoMimeTypes;

        //if (tagTimes == null) {
        //    tagTimes = new HashMap<>();
        //}
        //this.tagsTimes = tagTimes;
        //this.companionView = companionView;
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

    public String getAdTagURL() {
        return adTagURL;
    }

    public void setAdTagURL(String adTagURL) {
        this.adTagURL = adTagURL;
    }

    public boolean getAdAttribution() {
        return adAttribution;
    }
    public void setAdAttribution(boolean adAttribution) {
        this.adAttribution = adAttribution;
    }

    public boolean getAdCountDown() {
        return adCountDown;
    }

    public void setAdCountDown(boolean adCountDown) {
        this.adCountDown = adCountDown;
    }

    public boolean isPlayOnAdResume() {
        return playOnAdResume;
    }

    public void setPlayOnAdResume(boolean playOnAdResume) {
        this.playOnAdResume = playOnAdResume;
    }

    //    public Map<Double, String> getTagsTimes() {
//        return tagsTimes;
//    }
//
//    public void setTagsTimes(Map<Double, String> tagsTimes) {
//        this.tagsTimes = tagsTimes;
//    }

//    public View getCompanionView() {
//        return companionView;
//    }
//
//    public void setCompanionView(View companionView) {
//        this.companionView = companionView;
//    }
//

    public JsonObject toJSONObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(AD_TAG_LANGUAGE, language);
        jsonObject.addProperty(AD_TAG_URL, adTagURL);
        jsonObject.addProperty(ENABLE_BG_PLAYBACK, enableBackgroundPlayback);
        jsonObject.addProperty(AUTO_PLAY_AD_BREAK, autoPlayAdBreaks);
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, adAttribution);
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, adCountDown);
        jsonObject.addProperty(AD_PLAY_ON_RESUME, playOnAdResume);

        Gson gson = new Gson();
        JsonArray jArray = new JsonArray();
        for (String mimeType : videoMimeTypes) {
            JsonPrimitive element = new JsonPrimitive(mimeType);
            jArray.add(element);
        }
        jsonObject.add(AD_VIDEO_MIME_TYPES, jArray);

//        String tagsTimesJsonString = gson.toJson(tagsTimes);
//        if (tagsTimesJsonString != null && !tagsTimesJsonString.isEmpty()) {
//            JsonParser parser = new JsonParser();
//            JsonObject tagsTimesJsonObject = parser.parse(tagsTimesJsonString).getAsJsonObject();
//            jsonObject.add(AD_TAG_TIMES, tagsTimesJsonObject);
//        } else {
//            jsonObject.add(AD_TAG_TIMES, new JsonObject());
//        }

        return jsonObject;
    }
}

