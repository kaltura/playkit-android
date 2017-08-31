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

package com.kaltura.playkit.plugins.ads.kaltura;

import android.view.View;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.ads.AdTagType;


public class ADConfig {

    public static final int DEFAULT_AD_LOAD_TIMEOUT = 8;

    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_TAG_TYPE = "adTagType";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String AD_VIDEO_MIME_TYPES      = "videoMimeTypes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT   = "adCountDown";
    public static final String AD_LOAD_TIMEOUT          = "adLoadTimeOut";
    public static final String AD_ENABLE_DEBUG_MODE     = "enableDebugMode";


    private String language;
    private String adTagURL;
    private View playerViewContainer;
    private View adSkinContainer;
    //View companionView;
    private long startPosition;
    private AdTagType adTagType;
    private int videoBitrate; // in KB
    private boolean adAttribution;
    private boolean adCountDown;
    //private boolean enableDebugMode;
    private int  adLoadTimeOut;
    private String videoMimeType;
    private int companionAdWidth;
    private int companionAdHeight;

    public ADConfig() {
        this.language                 = "en";
        this.adTagType = AdTagType.VAST;
        this.videoBitrate             = -1;
        this.adLoadTimeOut            = DEFAULT_AD_LOAD_TIMEOUT;
        //this.enableDebugMode          = false;
        this.videoMimeType =          PKMediaFormat.mp4.mimeType;
        this.adTagURL = null;         //=> must be set via setter
        this.playerViewContainer = null;
        this.adSkinContainer = null;
        this.startPosition = 0;
        this.companionAdWidth = 0;
        this.companionAdHeight = 0;

        //this.companionView = companionView;
    }

    public String getLanguage() {
        return language;
    }

    // Language - default is en.
    public ADConfig setLanguage(String language) {
        this.language = language;
        return this;
    }

    public ADConfig setAdTagType(AdTagType adTagType) {
        this.adTagType = adTagType;
        return this;
    }

    public ADConfig setStartPosition(long startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public ADConfig setPlayerViewContainer(View playerViewContainer) {
        this.playerViewContainer = playerViewContainer;
        return this;
    }

    public ADConfig setAdSkinContainer(View adSkinContainer) {
        this.adSkinContainer = adSkinContainer;
        return this;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    // Maximum recommended bitrate. The value is in kbit/s.
    public ADConfig setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
        return this;
    }

    public String getVideoMimeType() {
        return videoMimeType;
    }

    // select the MIME TYPE that IMA will play instead of letting it select it by itself
    // default selected MIME TYPE by plugin is MP4
    // if null or empty list is set then it will be selected automatically
    // if MIME TYPE is sent it will try playing one of the given MIME TYPE in the list i.e "video/mp4", "video/webm", "video/3gpp"
    public ADConfig setVideoMimeTypes(String videoMimeType) {
        this.videoMimeType = videoMimeType;
        return this;
    }

    public String getAdTagURL() {
        return adTagURL;
    }

    // set the adTag URL to be used
    public ADConfig setAdTagURL(String adTagURL) {
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
    public ADConfig setAdAttribution(boolean adAttribution) {
        this.adAttribution = adAttribution;
        return this;
    }

    public boolean getAdCountDown() {
        return adCountDown;
    }

    // set if ad countdown will be shown or not.
    // default is true
    public ADConfig setAdCountDown(boolean adCountDown) {
        this.adCountDown = adCountDown;
        return this;
    }

    public int getAdLoadTimeOut() {
        return adLoadTimeOut;
    }

    public ADConfig setAdLoadTimeOut(int adLoadTimeOut) {
        this.adLoadTimeOut = adLoadTimeOut;
        return this;
    }

    public ADConfig setCompanionAdWidth(int companionAdWidth) {
        this.companionAdWidth = companionAdWidth;
        return this;
    }

    public ADConfig setCompanionAdHeight(int companionAdHeight) {
        this.companionAdHeight = companionAdHeight;
        return this;
    }

    //public ADConfig enableDebugMode(boolean enableDebugMode) {
    //    this.enableDebugMode = enableDebugMode;
    //    return this;
    //}

    //public boolean isDebugMode() {
    //    return enableDebugMode;
    //}

    public View getPlayerViewContainer() {
        return playerViewContainer;
    }

    public View getAdSkinContainer() {
        return adSkinContainer;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public int getCompanionAdWidth() {
        return companionAdWidth;
    }

    public int getCompanionAdHeight() {
        return companionAdHeight;
    }

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
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, adAttribution);
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, adCountDown);
        jsonObject.addProperty(AD_LOAD_TIMEOUT, adLoadTimeOut);
        //jsonObject.addProperty(AD_ENABLE_DEBUG_MODE, enableDebugMode);
        jsonObject.addProperty(AD_VIDEO_MIME_TYPES, videoMimeType);

        return jsonObject;
    }
}


