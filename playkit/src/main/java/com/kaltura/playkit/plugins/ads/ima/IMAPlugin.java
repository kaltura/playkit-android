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

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.UiElement;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.AdTagType;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.player.PlayerSettings;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdPositionType;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_ENDED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_STARTED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_PROGRESS;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAPlugin extends PKPlugin implements AdsProvider, com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener {

    private static final PKLog log = PKLog.get("IMAPlugin");

    /////////////////////
    private Player player;
    private Context context;
    private AdInfo adInfo;
    private IMAConfig adConfig;
    private PKAdProviderListener pkAdProviderListener;
    private PKMediaConfig mediaConfig;

    // The container for the ad's UI.
    private ViewGroup adUiContainer;

    // Factory class for creating SDK objects.
    private ImaSdkFactory sdkFactory;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader adsLoader;
    private AdsLoader.AdsLoadedListener adsLoadedListener;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private ImaSdkSettings imaSdkSettings;
    private AdsManager adsManager;
    private AdsRenderingSettings renderingSettings;
    private AdCuePoints adTagCuePoints;
    // Whether an ad is displayed.
    private boolean isAdDisplayed;
    private boolean isAdIsPaused;
    private boolean isAdRequested;
    private boolean isInitWaiting;
    private boolean isAutoPlay;

    private boolean appIsInBackground;
    private boolean adManagerInitDuringBackground;
    private boolean appInBackgroundDuringAdLoad;

    ////////////////////
    private MessageBus messageBus;

    private AdDisplayContainer adDisplayContainer;
    private CountDownTimer adManagerTimer;
    private boolean adPlaybackCancelled;
    private Timer adDisplayedCheckTimer;
    private boolean isContentPrepared;
    private boolean isAllAdsCompleted;
    private boolean isContentEndedBeforeMidroll;
    private boolean isAdError;
    private PlayerEvent.Type lastPlaybackPlayerState;
    private com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType lastAdEventReceived;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "IMA";
        }

        @Override
        public PKPlugin newInstance() {
            return new IMAPlugin();
        }

        @Override
        public void warmUp(Context context) {
            log.d("warmUp started");
            ImaSdkFactory.getInstance().createAdsLoader(context);
        }
    };

    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }

    @Override
    protected void onLoad(final Player player, Object config, final MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        this.isAllAdsCompleted = false;

        if (this.messageBus == null) {
            this.messageBus = messageBus;
            this.messageBus.listen(new PKEvent.Listener() {
                @Override
                public void onEvent(PKEvent event) {
                    log.d("Received:PlayerEvent:" + event.eventType().name() + " lastAdEventReceived = " + lastAdEventReceived);
                    AdCuePoints adCuePoints = new AdCuePoints(getAdCuePoints());
                    if (event.eventType() == PlayerEvent.Type.ENDED) {
                        lastPlaybackPlayerState = PlayerEvent.Type.ENDED;
                        if (adInfo != null) {
                            log.d("ENDED adInfo.getAdIndexInPod() = " + adInfo.getAdIndexInPod() + " -  adInfo.getTotalAdsInPod() = " + adInfo.getTotalAdsInPod());
                        }
                        boolean isLastMidrollPlayed = !adCuePoints.hasMidRoll() || (adCuePoints.getAdCuePoints().size() >= 2 && adCuePoints.hasPostRoll() && adInfo != null && adInfo.getAdPodTimeOffset() == adCuePoints.getAdCuePoints().get(adCuePoints.getAdCuePoints().size()-2));
                        log.d("contentCompleted isLastMidrollPlayed = " + isLastMidrollPlayed);

                        if (!isAdDisplayed && (!adCuePoints.hasPostRoll() || isAllAdsCompleted || isLastMidrollPlayed)) {
                            log.d("contentCompleted on ended");
                            contentCompleted();
                        } else {
                            log.d("contentCompleted delayed");
                            isContentEndedBeforeMidroll = true;
                        }
                    } else if(event.eventType() == PlayerEvent.Type.PLAYING) {
                        if (mediaConfig != null && mediaConfig.getMediaEntry() != null) {
                            //log.d("PlayerDuration = " + player.getDuration());
                            //log.d("PlayerDuration Metadata = " + mediaConfig.getMediaEntry().getDuration());
                            mediaConfig.getMediaEntry().setDuration(player.getDuration());
                            lastAdEventReceived = null;
                        }
                    }
                }
            }, PlayerEvent.Type.ENDED, PlayerEvent.Type.PLAYING);
        }

        adConfig = parseConfig(config);
        adUiContainer = player.getView();
    }

    private static IMAConfig parseConfig(Object config) {
        if (config instanceof IMAConfig) {
            return ((IMAConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), IMAConfig.class);
        }
        return null;
    }

    private void imaSetup() {
        log.d("imaSetup start");
        imaSettingSetup();
        if (sdkFactory == null) {
            sdkFactory = ImaSdkFactory.getInstance();
        }
        if (adsLoader == null) {
            adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings);
            // Add listeners for when ads are loaded and for errors.
            adsLoader.addAdErrorListener(this);
            adsLoader.addAdsLoadedListener(getAdsLoadedListener());


            adDisplayContainer = sdkFactory.createAdDisplayContainer();
            adDisplayContainer.setAdContainer(adUiContainer);

            renderingSettings = sdkFactory.createAdsRenderingSettings();
            if (mediaConfig != null && mediaConfig.getStartPosition() > 0) {
                renderingSettings.setPlayAdsAfterTime(mediaConfig.getStartPosition());
            }

            if (adConfig.getVideoMimeTypes() != null && adConfig.getVideoMimeTypes().size() > 0) {
                renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
            } else {
                List<String> defaultMimeType = new ArrayList<>();
                defaultMimeType.add(PKMediaFormat.mp4.mimeType);
                renderingSettings.setMimeTypes(defaultMimeType);
            }

            //if both are false we remove the support int ad count down in ad
            if (!adConfig.getAdAttribution() && !adConfig.getAdCountDown()) {
                renderingSettings.setUiElements(Collections.<UiElement>emptySet());
            }

            if (adConfig.getVideoBitrate() != -1) {
                renderingSettings.setBitrateKbps(adConfig.getVideoBitrate());
            }
        }
    }

    private void imaSettingSetup() {
        if (imaSdkSettings == null) {
           imaSdkSettings = ImaSdkFactory.getInstance().createImaSdkSettings();
        }
        // Tell the SDK we want to control ad break playback.
        //imaSdkSettings.setAutoPlayAdBreaks(true);
        if (adConfig.getMaxRedirects() > 0) {
            imaSdkSettings.setMaxRedirects(adConfig.getMaxRedirects());
        }

        imaSdkSettings.setLanguage(adConfig.getLanguage());
        imaSdkSettings.setDebugMode(adConfig.isDebugMode());
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("Start onUpdateMedia");
        this.mediaConfig = mediaConfig;
        isContentPrepared = false;
        isAutoPlay = false;
        isAdRequested = false;
        isAdDisplayed = false;
        isAllAdsCompleted = false;
        isContentEndedBeforeMidroll = false;
        lastPlaybackPlayerState = null;
        lastAdEventReceived = null;
        if (adsManager != null) {
            adsManager.destroy();
        }
        clearAdsLoader();
        imaSetup();
        String imaAdTag = adConfig.getAdTagURL();
        requestAdsFromIMA(imaAdTag);
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("Start onUpdateConfig");
        adConfig = parseConfig(config);
    }

    private void clearAdsLoader() {
        if (adsLoader != null) {
            adsLoader.removeAdErrorListener(this);
            adsLoader.removeAdsLoadedListener(adsLoadedListener);
            adsLoadedListener = null;
            adsLoader = null;
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");
        if (player != null) {
            if (!isAdDisplayed) {
                if (player.isPlaying()) {
                    lastPlaybackPlayerState = PlayerEvent.Type.PLAYING;
                } else {
                    lastPlaybackPlayerState = PlayerEvent.Type.PAUSE;
                }
            } else {
                lastPlaybackPlayerState = PlayerEvent.Type.PAUSE;
            }
        }
        appIsInBackground = true;
        if (adsManager == null) {
            cancelAdManagerTimer();
            cancelAdDisplayedCheckTimer();
        }
        pause();
        if (adsManager != null && !isContentPrepared) {
            resetIMA();
            return;
        }
    }

    @Override
    protected void onApplicationResumed() {
        log.d("onApplicationResumed lastAdEventReceived => " + lastAdEventReceived);
        if (lastAdEventReceived == null) {
            if (isContentPrepared) {

                player.play();
                clearOnApplicationPausedData();
                return;
            }

            isInitWaiting = false;
            appIsInBackground = false;
            adManagerInitDuringBackground = false;
            appInBackgroundDuringAdLoad = false;
            onUpdateMedia(mediaConfig);
            initAdDisplayedCheckTimer();
            start();
            return;
        }

        //for resuming before preroll played
        if (adsManager != null && adManagerInitDuringBackground) {
            log.d("onApplicationResumed adsManager.init");
            player.getView().hideVideoSurface();
            adsManager.init(renderingSettings);
            sendCuePointsUpdate();
            clearOnApplicationPausedData();
            return;
        }

        if (adsManager != null) {
            log.d("onApplicationResumed - adsManager != null");
            switch (lastAdEventReceived) {
                case PAUSED:
                    player.getView().hideVideoSurface();
                    if(player.getSettings() instanceof PlayerSettings && ((PlayerSettings)player.getSettings()).isAdAutoPlayOnResume()) {
                        adsManager.resume();
                    }
                    break;
                case CONTENT_PAUSE_REQUESTED:
                    player.getView().hideVideoSurface();
                    adsManager.resume();
                    break;
                case LOADED:
                    player.getView().hideVideoSurface();
                    adsManager.resume();

                    if (adPlaybackCancelled) {
                        log.d("discarding ad break");
                        adsManager.discardAdBreak();
                    } else {
                        messageBus.post(new AdEvent.AdLoadedEvent(adInfo));
                        if (AdTagType.VMAP != adConfig.getAdTagType()) {
                            adsManager.start();
                        }
                    }
                    break;
                case CONTENT_RESUME_REQUESTED:
                    if (isContentPrepared && player != null && mediaConfig != null && mediaConfig.getMediaEntry() != null) {
                        log.d("CONTENT_RESUME_REQUESTED onApplicationResumed - player.getDuration() = " + player.getDuration());
                        log.d("CONTENT_RESUME_REQUESTED onApplicationResumed - player.getCurrentPosition() = " + player.getCurrentPosition());
                        if(isContentEndedBeforeMidroll || player.getCurrentPosition() >= mediaConfig.getMediaEntry().getDuration()) {
                            log.d("CONTENT_RESUME_REQUESTED onApplicationResumed - contentCompleted");
                            //contentCompleted();
                            adsManager.resume();
                        } else {
                            log.d("CONTENT_RESUME_REQUESTED onApplicationResumed - play lastPlayerState = " +  lastPlaybackPlayerState);
                            if (!isAdDisplayed || lastPlaybackPlayerState == PlayerEvent.Type.PLAYING) {
                                player.play();
                            }
                        }
                    } else {
                        if (!isContentPrepared) {
                            preparePlayer(true);
                        } else {
                            if (player != null) {
                                player.play();
                            }
                        }
                    }

                    break;
                case ALL_ADS_COMPLETED:
                    if (player != null && lastPlaybackPlayerState == PlayerEvent.Type.PLAYING) {
                        player.play();
                    }
                    break;
                default:
                    log.d("onApplicationResumed - default");
                    if (isAdDisplayed) {
                        if(player.getSettings() instanceof PlayerSettings && ((PlayerSettings)player.getSettings()).isAdAutoPlayOnResume()) {
                            adsManager.resume();
                        }
                    } else {
                        //if ad is not displayed so play
                        log.d("onApplicationResumed: lastAdEventReceived = " + lastAdEventReceived + " isAdDisplayed = " + isAdDisplayed);
                        if (player != null && lastPlaybackPlayerState == PlayerEvent.Type.PLAYING) {
                            player.play();
                        }
                    }
            }
        } else {
            if (adsManager == null && !isContentPrepared && mediaConfig != null) {
                log.d("onApplicationResumed - !isContentPrepared && mediaConfig != null");
                if (adConfig != null) {
                    log.d("IMA on resume restart");
                    isInitWaiting = true;
                    appIsInBackground = false;
                    adManagerInitDuringBackground = false;
                    appInBackgroundDuringAdLoad = false;
                    initAdDisplayedCheckTimer();
                    onUpdateMedia(mediaConfig);
                    start();
                    return;
                }
            } else {
                log.d("prepare lastPlaybackPlayerState = " + lastPlaybackPlayerState);
                if (player != null && lastPlaybackPlayerState == PlayerEvent.Type.PLAYING) {
                    player.play();
                }
            }
        }
        clearOnApplicationPausedData();
    }

    private void clearOnApplicationPausedData() {
        isInitWaiting = false;
        appIsInBackground = false;
        adManagerInitDuringBackground = false;
        appInBackgroundDuringAdLoad = false;
        isContentEndedBeforeMidroll = false;
    }
    @Override
    public void destroyAdsManager() {
        if (adsManager == null) {
            return;
        }
        log.d("IMA Start destroyAdsManager");
        contentCompleted();
        adsManager.destroy();
        adsManager = null;
        isAdRequested = false;
        isAdDisplayed = false;
        adPlaybackCancelled = false;
    }

    @Override
    protected void onDestroy() {
        log.d("IMA Start onDestroy");
        resetIMA();
        clearAdsLoader();
        sdkFactory = null;
        imaSdkSettings = null;

        isContentPrepared = false;
        removeAdProviderListener();
    }

    protected void resetIMA() {
        log.d("Start resetIMA");
        isAdError = false;
        isAdRequested = false;
        isAdDisplayed = false;
        lastPlaybackPlayerState = null;
        lastAdEventReceived = null;

        cancelAdDisplayedCheckTimer();
        cancelAdManagerTimer();

        adTagCuePoints = null;
        adPlaybackCancelled = false;
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
    }

    private void cancelAdManagerTimer() {
        if (adManagerTimer != null) {
            log.d("cancelAdManagerTimer");
            adManagerTimer.cancel();
            adManagerTimer = null;
        }
    }

    private AdsLoader.AdsLoadedListener getAdsLoadedListener() {
        if (adsLoadedListener != null) {
            return adsLoadedListener;
        }
        adsLoadedListener = new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {

                log.d("AdsManager loaded");

                cancelAdManagerTimer();
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                adsManager = adsManagerLoadedEvent.getAdsManager();
                sendCuePointsUpdateEvent();
                //Attach event and error event listeners.

                adsManager.addAdErrorListener(IMAPlugin.this);
                adsManager.addAdEventListener(IMAPlugin.this);

                if (isInitWaiting) {
                    if (appIsInBackground) {
                        log.d("IMA app in background return");
                        adManagerInitDuringBackground = true;
                        return;
                    }
                    log.d("IMA adsManager.init called");
                    adsManager.init(renderingSettings);
                    isInitWaiting = false;
                }

            }
        };
        return adsLoadedListener;
    }

    private void sendCuePointsUpdateEvent() {
        adTagCuePoints = new AdCuePoints(getAdCuePoints());
        messageBus.post(new AdEvent.AdCuePointsUpdateEvent(adTagCuePoints));
    }

    @Override
    public void start() {
        log.d("Start adsManager.init");
        isAutoPlay = true; // start will be called only on first time media is played programmatically
        isAdRequested = true;
        if (adsManager != null) {
            log.d("IMA adsManager.init called");
            if (appIsInBackground) {
                log.d("Start: Ad Manager Init : " + adManagerInitDuringBackground);
                adManagerInitDuringBackground = true;
            } else {
                adsManager.init(renderingSettings);
            }
        } else {
            isInitWaiting = true;
        }
    }

    private void requestAdsFromIMA(String adTagUrl) {
        log.d("Do requestAdsFromIMA adTagUrl = " + adTagUrl);

        if (TextUtils.isEmpty(adTagUrl)) {
            log.d("AdTag is empty avoiding ad request");
            isAdRequested = true;
            preparePlayer(false);
            return;
        }

        resetIMA();
        // Create the ads request.
        final AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        if (adConfig.getAdLoadTimeOut() > 0 && adConfig.getAdLoadTimeOut() < Consts.MILLISECONDS_MULTIPLIER && adConfig.getAdLoadTimeOut() != IMAConfig.DEFAULT_AD_LOAD_TIMEOUT) {
            request.setVastLoadTimeout(adConfig.getAdLoadTimeOut() * Consts.MILLISECONDS_MULTIPLIER);
        }
        request.setAdDisplayContainer(adDisplayContainer);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (adsManager == null || player == null) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();

                if (isAdDisplayed || currentPosition < 0 || duration <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(currentPosition, duration);
            }
        });
        adManagerTimer = new CountDownTimer(adConfig.getAdLoadTimeOut() * Consts.MILLISECONDS_MULTIPLIER, IMAConfig.DEFAULT_AD_LOAD_COUNT_DOWN_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                log.d("adManagerTimer.onTick, adsManager=" + adsManager);
                if (adsManager != null) {
                    log.d("cancelling adManagerTimer");
                    this.cancel();
                }
            }

            @Override
            public void onFinish() {
                log.d("adManagerTimer.onFinish, adsManager=" + adsManager);
                if (adsManager == null) {
                    log.d("adsManager is null, will play content");
                    preparePlayer(true);

                    messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_IGNORED));
                    if (isAdRequested) {
                        adPlaybackCancelled = true;
                    }
                }
            }
        };

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        messageBus.post(new AdEvent.AdRequestedEvent(adTagUrl));
        adsLoader.requestAds(request);
        adManagerTimer.start();
    }

    @Override
    public void resume() {
        log.d("AD Event resume isAdDisplayed = " + isAdDisplayed);
        if (adsManager != null) {
            if (isAdDisplayed) {
                adsManager.resume();
            }
        }
    }

    @Override
    public void pause() {
        log.d("AD Event pause isAdDisplayed = " + isAdDisplayed);
        if (player != null) {
            player.pause();
        }
        if (adsManager != null) {// && isAdDisplayed) {
            adsManager.pause();
        }
    }

    @Override
    public void contentCompleted() {
        if (adsManager != null && adsLoader != null) {
            log.d("contentCompleted");
            adsLoader.contentComplete();
        }
    }

    @Override
    public PKAdInfo getAdInfo() {
        return adInfo;
    }

    @Override
    public boolean isAdDisplayed() {
        //log.d("isAdDisplayed: " + mIsAdDisplayed);
        return isAdDisplayed;
    }

    @Override
    public boolean isAdError() {
        return isAdError;
    }

    @Override
    public boolean isAdPaused() {
        log.d("isAdPaused: " + isAdIsPaused);
        return isAdIsPaused;
    }

    @Override
    public boolean isAdRequested() {
        log.d("isAdRequested: " + isAdRequested);
        return isAdRequested;
    }

    @Override
    public boolean isAllAdsCompleted() {
        return isAllAdsCompleted;
    }

    @Override
    public long getDuration() {
        if (adsManager != null) {
            return (long) adsManager.getAdProgress().getDuration();
        }

        return Consts.TIME_UNSET;
    }

    @Override
    public long getCurrentPosition() {
        if (adsManager != null) {
            long adCurrentPos = (long) adsManager.getAdProgress().getCurrentTime();
            messageBus.post(new AdEvent.AdPlayHeadEvent(adCurrentPos));
            return adCurrentPos;
        }

        return Consts.POSITION_UNSET;
    }

    @Override
    public void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController) {
        pkAdProviderListener = adEnabledPlayerController;
    }

    @Override
    public void removeAdProviderListener() {
        pkAdProviderListener = null;
    }

    @Override
    public void skipAd() {
        if (adsManager != null) {
            adsManager.skip();
        }
    }

    @Override
    public void onAdEvent(com.google.ads.interactivemedia.v3.api.AdEvent adEvent) {
        lastAdEventReceived = adEvent.getType();
        log.d("Event Name => " + lastAdEventReceived.name());
        if (adEvent.getAdData() != null) {
            log.d("Event: " + adEvent.getAdData().toString());
        }

        if (adsManager == null) {
            return;
        }
        switch (lastAdEventReceived) {

            case LOADED:
                log.d("LOADED appIsInBackground = " + appIsInBackground);

                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                if (appIsInBackground) {
                    appInBackgroundDuringAdLoad = true;
                    if (adsManager != null) {
                        log.d("LOADED call   adsManager.pause()");
                        adsManager.pause();
                    }
                } else {
                    adInfo = createAdInfo(adEvent.getAd());
                    log.d("podInfo.getAdPosition() = " + adInfo.getAdIndexInPod());
                    log.d("getTotalAds() = " + adInfo.getTotalAdsInPod());

                    if (adInfo.getAdIndexInPod() == 1) {
                        player.getView().hideVideoSurface();
                    }

                    if (adPlaybackCancelled) {
                        log.d("discarding ad break");
                        adsManager.discardAdBreak();
                    } else {
                        messageBus.post(new AdEvent.AdLoadedEvent(adInfo));
                        if (AdTagType.VMAP != adConfig.getAdTagType()) {
                            adsManager.start();
                        }
                    }
                }
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                log.d("AD_CONTENT_PAUSE_REQUESTED appIsInBackground = " + appIsInBackground);

                if (appIsInBackground) {
                    appInBackgroundDuringAdLoad = true;
                    if (adsManager != null) {
                        adsManager.pause();
                    }
                }

                if (!adPlaybackCancelled) {
                    messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));

                    if (player != null) {
                        player.pause();
                    }
                }
                initAdDisplayedCheckTimer();
                isAdDisplayed = true;
                break;
            case CONTENT_RESUME_REQUESTED:
                log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED");

                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                if (isContentEndedBeforeMidroll && !isAllAdsCompleted && player.getCurrentPosition() >= player.getDuration()) {
                    log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED - contentCompleted");
                    contentCompleted();
                    return;
                }
                player.getView().showVideoSurface();
                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
                isAdDisplayed = false;
                cancelAdDisplayedCheckTimer();

                if (!isContentPrepared) {
                    log.d("Content not prepared.. Preparing and calling play.");
                    if (pkAdProviderListener != null && !appIsInBackground) {
                        preparePlayer(true);
                    }
                } else if (player != null) {
                    log.d("Content prepared..");
                    player.getView().showVideoSurface();
                    long duration = player.getDuration();
                    if (lastPlaybackPlayerState != PlayerEvent.Type.ENDED && (duration < 0 || player.getCurrentPosition() <= duration)) {
                        if (adInfo == null || (adInfo != null && adInfo.getAdPositionType() != AdPositionType.POST_ROLL)) {
                            log.d("Content prepared.. Play called.");
                            player.play();
                        }
                    }
                }
                adPlaybackCancelled = false;
                break;
            case ALL_ADS_COMPLETED:
                log.d("AD_ALL_ADS_COMPLETED");
                isAllAdsCompleted = true;
                isAdDisplayed = false;
                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                player.getView().showVideoSurface();
                if (adInfo != null && adInfo.getAdPositionType() == AdPositionType.POST_ROLL) {
                    contentCompleted();
                }
                if (adsManager != null) {
                    log.d("AD_ALL_ADS_COMPLETED resetIMA");
                    resetIMA();
                }

                break;
            case STARTED:
                log.d("AD STARTED");
                isAdDisplayed = true;
                isAdIsPaused = false;
                player.getView().hideVideoSurface();
                if (adsManager != null && appIsInBackground) {
                    log.d("AD STARTED and pause");
                    adsManager.pause();
                }

                adInfo = createAdInfo(adEvent.getAd());
                messageBus.post(new AdEvent.AdStartedEvent(adInfo));

                preparePlayer(false);

                if (adTagCuePoints == null) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            log.d("AD CUEPOINTS CHANGED TRIGGERED WITH DELAY");
                            sendCuePointsUpdateEvent();
                        }
                    }, IMAConfig.DEFAULT_CUE_POINTS_CHANGED_DELAY);
                }
                break;
            case PAUSED:
                log.d("AD PAUSED");
                isAdIsPaused = true;
                adInfo.setAdPlayHead(getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER);
                messageBus.post(new AdEvent.AdPausedEvent(adInfo));
                break;
            case RESUMED:
                log.d("AD RESUMED");
                if (player != null && player.getView() != null) {
                    player.getView().hideVideoSurface();
                }
                isAdIsPaused = false;
                adInfo.setAdPlayHead(getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER);
                messageBus.post(new AdEvent.AdResumedEvent(adInfo));
                break;
            case COMPLETED:
                log.d("AD COMPLETED");
                messageBus.post(new AdEvent(AdEvent.Type.COMPLETED));
                cancelAdDisplayedCheckTimer();
                break;
            case FIRST_QUARTILE:
                messageBus.post(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
                break;
            case MIDPOINT:
                messageBus.post(new AdEvent(AdEvent.Type.MIDPOINT));
                break;
            case THIRD_QUARTILE:
                messageBus.post(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
                break;
            case SKIPPED:
                adInfo.setAdPlayHead(getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER);
                messageBus.post(new AdEvent.AdSkippedEvent(adInfo));
                //cancelAdDisplayedCheckTimer();
                //preparePlayer(true);
                break;
            case CLICKED:
                isAdIsPaused = true;
                messageBus.post(new AdEvent(AdEvent.Type.CLICKED));
                break;
            case TAPPED:
                messageBus.post(new AdEvent(AdEvent.Type.TAPPED));
                break;
            case ICON_TAPPED:
                messageBus.post(new AdEvent(AdEvent.Type.ICON_TAPPED));
                break;
            case AD_BREAK_READY:
                log.d("AD_BREAK_READY adPlaybackCancelled = " + adPlaybackCancelled);
                if (adPlaybackCancelled) {
                    log.d("discarding ad break");
                    adsManager.discardAdBreak();
                } else {
                    adsManager.start();
                }
                messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_READY));
                break;
            case AD_PROGRESS:
                messageBus.post(new AdEvent(AD_PROGRESS));
                break;
            case AD_BREAK_STARTED:
                messageBus.post(new AdEvent(AD_BREAK_STARTED));
                break;
            case AD_BREAK_ENDED:
                messageBus.post(new AdEvent(AD_BREAK_ENDED));
                break;
            case CUEPOINTS_CHANGED:
                sendCuePointsUpdate();
                break;
            case LOG:
                isAdRequested = true;
                //for this case no AD ERROR is fired need to show view {type=adLoadError, errorCode=1009, errorMessage=The response does not contain any valid ads.}
                preparePlayer(false);
                if (!isAdDisplayed) {
                    cancelAdDisplayedCheckTimer();
                }
                Ad adInfo = adEvent.getAd();
                if (adInfo != null) {
                    //incase one ad in the pod fails to play we want next one to be played
                    AdPodInfo adPodInfo = adInfo.getAdPodInfo();
                    log.d("adPodInfo.getAdPosition() = " + adPodInfo.getAdPosition() + " adPodInfo.getTotalAds() = " + adPodInfo.getTotalAds());
                    if (adPodInfo.getTotalAds() > 1 && adPodInfo.getAdPosition() < adPodInfo.getTotalAds()) {
                        log.d("LOG Error but continue to next ad in pod");
                        return;
                    } else {
                        adsManager.discardAdBreak();
                    }
                }

                String error = "Non-fatal Error";
                if (adEvent.getAdData() != null) {
                    if (adEvent.getAdData().containsKey("errorMessage")) {
                        error = adEvent.getAdData().get("errorMessage");
                    }
                }

                sendError(PKAdErrorType.QUIET_LOG_ERROR, error, null);
                break;
            default:
                break;
        }
    }

    private void preparePlayer(boolean doPlay) {
        log.d("IMA prepare");
        if (pkAdProviderListener != null && !appIsInBackground) {
            log.d("IMA prepare player");
            isContentPrepared = true;
            pkAdProviderListener.onAdLoadingFinished();
            if (doPlay) {
                messageBus.listen(new PKEvent.Listener() {
                    @Override
                    public void onEvent(PKEvent event) {
                        log.d("IMA DURATION_CHANGE received calling play");
                        if (player != null && player.getView() != null && !isAdDisplayed()) {
                            player.getView().showVideoSurface();
                            player.play();
                        }

                        messageBus.remove(this, PlayerEvent.Type.DURATION_CHANGE);
                    }
                }, PlayerEvent.Type.DURATION_CHANGE);
            }
        }
    }

    private void initAdDisplayedCheckTimer() {
        cancelAdDisplayedCheckTimer();
        adDisplayedCheckTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (adsManager != null && adsManager.getAdProgress() != null) {
                    float currentTime = adsManager.getAdProgress().getCurrentTime();
                    if (currentTime > 0) {
                        log.d("AD Displayed delay check : ad posiiton " + currentTime);
                        isAdDisplayed = true;
                        messageBus.post(new AdEvent(AD_PROGRESS));
                        cancelAdDisplayedCheckTimer();
                    }
                }
            }
        };

        if (adDisplayedCheckTimer != null) {
            adDisplayedCheckTimer.schedule(timerTask, 0, IMAConfig.DEFAULT_AD_LOAD_COUNT_DOWN_TICK);
        }
    }

    private void cancelAdDisplayedCheckTimer() {
        if (adDisplayedCheckTimer != null) {
            adDisplayedCheckTimer.cancel();
            adDisplayedCheckTimer = null;
        }
    }

    private void sendCuePointsUpdate() {
        List<Long> cuePoints = getAdCuePoints();
        if (cuePoints.size() > 0) {
            messageBus.post(new AdEvent.AdCuePointsUpdateEvent(new AdCuePoints(cuePoints)));
        }
    }

    private List<Long> getAdCuePoints() {
        List<Long> adCuePoints = new ArrayList<>();
        if (adsManager == null) {
            return adCuePoints;
        }

        List<Float> adCuePointsFloat = adsManager.getAdCuePoints();
        if (adCuePointsFloat != null) {
            for (Float cuePoint : adCuePointsFloat) {
                if (cuePoint >= 0) {
                    adCuePoints.add(cuePoint.longValue() * Consts.MILLISECONDS_MULTIPLIER);
                } else {
                    adCuePoints.add(cuePoint.longValue());
                }
            }
        }
        return adCuePoints;
    }

    private AdInfo createAdInfo(Ad ad) {
        String adDescription = ad.getDescription();
        long adDuration = (long) (ad.getDuration() * Consts.MILLISECONDS_MULTIPLIER);
        long adPlayHead = getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER;
        String adTitle = ad.getTitle();
        boolean isAdSkippable = ad.isSkippable();
        String contentType = ad.getContentType();
        String adId = ad.getAdId();
        String adSystem = ad.getAdSystem();
        int adHeight = ad.getHeight();
        int adWidth = ad.getWidth();
        int totalAdsInPod = ad.getAdPodInfo().getTotalAds();
        int adIndexInPod = ad.getAdPodInfo().getAdPosition();   // index starts in 1
        int podCount = (adsManager != null && adsManager.getAdCuePoints() != null) ? adsManager.getAdCuePoints().size() : 0;
        int podIndex = (ad.getAdPodInfo().getPodIndex() >= 0) ? ad.getAdPodInfo().getPodIndex() + 1 : podCount; // index starts in 0
        boolean isBumper = ad.getAdPodInfo().isBumper();
        long adPodTimeOffset = (long) (ad.getAdPodInfo().getTimeOffset() * Consts.MILLISECONDS_MULTIPLIER);


        AdInfo adInfo = new AdInfo(adDescription, adDuration, adPlayHead,
                adTitle, isAdSkippable,
                contentType, adId,
                adSystem, adHeight,
                adWidth,
                totalAdsInPod,
                adIndexInPod,
                podIndex,
                podCount,
                isBumper,
                (adPodTimeOffset < 0) ? -1 : adPodTimeOffset);

        log.v("AdInfo: " + adInfo.toString());
        return adInfo;

    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {

        resetFlagsOnError();

        AdError adException = adErrorEvent.getError();
        String errorMessage = (adException == null) ? "Unknown Error" : adException.getMessage();
        Enum errorType = PKAdErrorType.UNKNOWN_ERROR;

        if (adException != null) {

            switch (adException.getErrorCode()) {
                case INTERNAL_ERROR:
                    errorType = PKAdErrorType.INTERNAL_ERROR;
                    break;
                case VAST_MALFORMED_RESPONSE:
                    errorType = PKAdErrorType.VAST_MALFORMED_RESPONSE;
                    break;
                case UNKNOWN_AD_RESPONSE:
                    errorType = PKAdErrorType.UNKNOWN_AD_RESPONSE;
                    break;
                case VAST_LOAD_TIMEOUT:
                    errorType = PKAdErrorType.VAST_LOAD_TIMEOUT;
                    break;
                case VAST_TOO_MANY_REDIRECTS:
                    errorType = PKAdErrorType.VAST_TOO_MANY_REDIRECTS;
                    break;
                case VIDEO_PLAY_ERROR:
                    errorType = PKAdErrorType.VIDEO_PLAY_ERROR;
                    break;
                case VAST_MEDIA_LOAD_TIMEOUT:
                    errorType = PKAdErrorType.VAST_MEDIA_LOAD_TIMEOUT;
                    break;
                case VAST_LINEAR_ASSET_MISMATCH:
                    errorType = PKAdErrorType.VAST_LINEAR_ASSET_MISMATCH;
                    break;
                case OVERLAY_AD_PLAYING_FAILED:
                    errorType = PKAdErrorType.OVERLAY_AD_PLAYING_FAILED;
                    break;
                case OVERLAY_AD_LOADING_FAILED:
                    errorType = PKAdErrorType.OVERLAY_AD_LOADING_FAILED;
                    break;
                case VAST_NONLINEAR_ASSET_MISMATCH:
                    errorType = PKAdErrorType.VAST_NONLINEAR_ASSET_MISMATCH;
                    break;
                case COMPANION_AD_LOADING_FAILED:
                    errorType = PKAdErrorType.COMPANION_AD_LOADING_FAILED;
                    break;
                case UNKNOWN_ERROR:
                    errorType = PKAdErrorType.UNKNOWN_ERROR;
                    break;
                case VAST_EMPTY_RESPONSE:
                    errorType = PKAdErrorType.VAST_EMPTY_RESPONSE;
                    break;
                case FAILED_TO_REQUEST_ADS:
                    errorType = PKAdErrorType.FAILED_TO_REQUEST_ADS;
                    break;
                case VAST_ASSET_NOT_FOUND:
                    errorType = PKAdErrorType.VAST_ASSET_NOT_FOUND;
                    break;
                case ADS_REQUEST_NETWORK_ERROR:
                    errorType = PKAdErrorType.ADS_REQUEST_NETWORK_ERROR;
                    break;
                case INVALID_ARGUMENTS:
                    errorType = PKAdErrorType.INVALID_ARGUMENTS;
                    break;
                case PLAYLIST_NO_CONTENT_TRACKING:
                    errorType = PKAdErrorType.PLAYLIST_NO_CONTENT_TRACKING;
                    break;
            }
            if (errorMessage == null) {
                errorMessage = "Error code = " + adException.getErrorCode();
            }
        }

        sendError(errorType, errorMessage, adException);
        preparePlayer(isAutoPlay);
    }

    private void resetFlagsOnError() {
        isAdError = true;
        adPlaybackCancelled = true;
        isAdRequested = true;
        isAdDisplayed = false;
        cancelAdDisplayedCheckTimer();
        cancelAdManagerTimer();
    }

    private void sendError(Enum errorType, String message, Throwable exception) {
        log.e("Ad Error: " + errorType.name() + " with message " + message);
        AdEvent errorEvent = new AdEvent.Error(new PKError(errorType, message, exception));
        messageBus.post(errorEvent);
    }
}
