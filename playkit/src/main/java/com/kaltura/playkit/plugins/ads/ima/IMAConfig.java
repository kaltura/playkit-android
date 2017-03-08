package com.kaltura.playkit.plugins.ads.ima;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.ads.AdResponseType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAConfig {

    public static final int DEFAULT_AD_LOAD_TIMEOUT = 13;
    public static final int DEFAULT_CUE_POINTS_CHANGED_DELAY = 2000;
    public static final int DEFAULT_AD_LOAD_COUNT_DOWN_TICK = 250;

    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_RESPONSE_TYPE     = "adResponseType";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String ENABLE_BG_PLAYBACK  = "enableBackgroundPlayback";
    public static final String AUTO_PLAY_AD_BREAK  = "autoPlayAdBreaks";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String AD_VIDEO_MIME_TYPES      = "videoMimeTypes";
    public static final String AD_TAG_TIMES             = "tagsTimes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT   = "adCountDown";
    public static final String AD_LOAD_TIMEOUT          = "adLoadTimeOut";


    private String language;
    private String adTagURL;
    private AdResponseType adResponseType;
    private boolean enableBackgroundPlayback;
    private boolean autoPlayAdBreaks;
    private int videoBitrate; // in KB
    private boolean adAttribution;
    private boolean adCountDown;
    private int  adLoadTimeOut;
    private List<String> videoMimeTypes;
    //private Map<Double,String> tagsTimes; // <AdTime,URL_to_execute>

    //View companionView;

    public IMAConfig() {
        this.language                 = "en";
        this.adResponseType           = AdResponseType.VAST;
        this.enableBackgroundPlayback = false;
        this.autoPlayAdBreaks         = true;
        this.videoBitrate             = -1;
        this.adAttribution            = true;
        this.adCountDown              = true;
        this.adLoadTimeOut            = DEFAULT_AD_LOAD_TIMEOUT;
        this.videoMimeTypes           = new ArrayList<>();
        this.videoMimeTypes.add(PKMediaFormat.mp4_clear.mimeType);
        this.adTagURL = null;         //=> must be set via setter

        //if (tagTimes == null) {
        //    tagTimes = new HashMap<>();
        //}
        //this.tagsTimes = tagTimes;
        //this.companionView = companionView;
    }

    public String getLanguage() {
        return language;
    }

    public IMAConfig setLanguage(String language) {
        this.language = language;
        return this;
    }

    public IMAConfig setAdResponseType(AdResponseType adResponseType) {
        this.adResponseType = adResponseType;
        return this;
    }
    public boolean getEnableBackgroundPlayback() {
        return enableBackgroundPlayback;
    }

    public IMAConfig setEnableBackgroundPlayback(boolean enableBackgroundPlayback) {
        this.enableBackgroundPlayback = enableBackgroundPlayback;
        return this;
    }

    public boolean getAutoPlayAdBreaks() {
        return autoPlayAdBreaks;
    }

    public IMAConfig setAutoPlayAdBreaks(boolean autoPlayAdBreaks) {
        this.autoPlayAdBreaks = autoPlayAdBreaks;
        return this;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public IMAConfig setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
        return this;
    }

    public List<String> getVideoMimeTypes() {
        return videoMimeTypes;
    }

    public IMAConfig setVideoMimeTypes(List<String> videoMimeTypes) {
        this.videoMimeTypes = videoMimeTypes;
        return this;
    }

    public String getAdTagURL() {
        return adTagURL;
    }

    public IMAConfig setAdTagURL(String adTagURL) {
        this.adTagURL = adTagURL;
        return this;
    }

    public boolean getAdAttribution() {
        return adAttribution;
    }

    public AdResponseType getAdResponseType() {
        return adResponseType;
    }
    public IMAConfig setAdAttribution(boolean adAttribution) {
        this.adAttribution = adAttribution;
        return this;
    }

    public boolean getAdCountDown() {
        return adCountDown;
    }

    public IMAConfig setAdCountDown(boolean adCountDown) {
        this.adCountDown = adCountDown;
        return this;
    }

    public int getAdLoadTimeOut() {
        return adLoadTimeOut;
    }

    public IMAConfig setAdLoadTimeOut(int adLoadTimeOut) {
        this.adLoadTimeOut = adLoadTimeOut;
        return this;
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
        jsonObject.addProperty(AD_RESPONSE_TYPE, adResponseType.name());
        jsonObject.addProperty(AD_TAG_URL, adTagURL);
        jsonObject.addProperty(ENABLE_BG_PLAYBACK, enableBackgroundPlayback);
        jsonObject.addProperty(AUTO_PLAY_AD_BREAK, autoPlayAdBreaks);
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, adAttribution);
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, adCountDown);
        jsonObject.addProperty(AD_LOAD_TIMEOUT, adLoadTimeOut);

        Gson gson = new Gson();
        JsonArray jArray = new JsonArray();
        if (videoMimeTypes != null) {
            for (String mimeType : videoMimeTypes) {
                JsonPrimitive element = new JsonPrimitive(mimeType);
                jArray.add(element);
            }
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

