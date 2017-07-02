/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ads.ima;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.ads.AdTagType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAConfig {

    public static final int DEFAULT_AD_LOAD_TIMEOUT = 5;
    public static final int DEFAULT_CUE_POINTS_CHANGED_DELAY = 2000;
    public static final int DEFAULT_AD_LOAD_COUNT_DOWN_TICK = 250;

    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_TAG_TYPE = "adTagType";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String ENABLE_BG_PLAYBACK  = "enableBackgroundPlayback";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String AD_VIDEO_MIME_TYPES      = "videoMimeTypes";
    //public static final String AD_TAG_TIMES             = "tagsTimes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT   = "adCountDown";
    public static final String AD_LOAD_TIMEOUT          = "adLoadTimeOut";
    public static final String AD_ENABLE_DEBUG_MODE     = "enableDebugMode";

    private String language;
    private String adTagURL;
    private AdTagType adTagType;
    private boolean enableBackgroundPlayback;
    private int videoBitrate; // in KB
    private boolean adAttribution;
    private boolean adCountDown;
    private boolean enableDebugMode;
    private int  adLoadTimeOut;
    private List<String> videoMimeTypes;
    //private Map<Double,String> tagsTimes; // <AdTime,URL_to_execute>

    //View companionView;

    public IMAConfig() {
        this.language                 = "en";
        this.adTagType = AdTagType.VAST;
        this.enableBackgroundPlayback = false;
        this.videoBitrate             = -1;
        this.adAttribution            = true;
        this.adCountDown              = true;
        this.adLoadTimeOut            = DEFAULT_AD_LOAD_TIMEOUT;
        this.enableDebugMode          = false;
        this.videoMimeTypes           = new ArrayList<>();
        this.videoMimeTypes.add(PKMediaFormat.mp4.mimeType);
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

    // Language - default is en.
    public IMAConfig setLanguage(String language) {
        this.language = language;
        return this;
    }

    public IMAConfig setAdTagType(AdTagType adTagType) {
        this.adTagType = adTagType;
        return this;
    }

    public boolean getEnableBackgroundPlayback() {
        return enableBackgroundPlayback;
    }

    // default is false
    public IMAConfig setEnableBackgroundPlayback(boolean enableBackgroundPlayback) {
        this.enableBackgroundPlayback = enableBackgroundPlayback;
        return this;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    // Maximum recommended bitrate. The value is in kbit/s.
    // The IMA SDK will pick media with bitrate below the specified max, or the closest bitrate if there is no media with lower bitrate found.
    // Default value, -1, means the bitrate will be selected by the IMA SDK.
    public IMAConfig setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
        return this;
    }

    public List<String> getVideoMimeTypes() {
        return videoMimeTypes;
    }

    // select the MIME TYPE that IMA will play instead of letting it select it by itself
    // default selected MIME TYPE by plugin is MP4
    // if null or empty list is set then it will be selected automatically
    // if MIME TYPE is sent it will try playing one of the given MIME TYPE in the list i.e "video/mp4", "video/webm", "video/3gpp"
    public IMAConfig setVideoMimeTypes(List<String> videoMimeTypes) {
        this.videoMimeTypes = videoMimeTypes;
        return this;
    }

    public String getAdTagURL() {
        return adTagURL;
    }

    // set the adTag URL to be used
    public IMAConfig setAdTagURL(String adTagURL) {
        this.adTagURL = adTagURL;
        return this;
    }

    public boolean getAdAttribution() {
        return adAttribution;
    }

    public AdTagType getAdTagType() {
        return adTagType;
    }


    //ad attribution true is required for a countdown timer to be displayed
    // default is true
    public IMAConfig setAdAttribution(boolean adAttribution) {
        this.adAttribution = adAttribution;
        return this;
    }

    public boolean getAdCountDown() {
        return adCountDown;
    }

    // set if ad countdown will be shown or not.
    // default is true
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

    public IMAConfig enableDebugMode(boolean enableDebugMode) {
        this.enableDebugMode = enableDebugMode;
        return this;
    }

    public boolean isDebugMode() {
        return enableDebugMode;
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
        jsonObject.addProperty(AD_TAG_TYPE, adTagType.name());
        jsonObject.addProperty(AD_TAG_URL, adTagURL);
        jsonObject.addProperty(ENABLE_BG_PLAYBACK, enableBackgroundPlayback);
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, adAttribution);
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, adCountDown);
        jsonObject.addProperty(AD_LOAD_TIMEOUT, adLoadTimeOut);
        jsonObject.addProperty(AD_ENABLE_DEBUG_MODE, enableDebugMode);

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


