package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
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
import com.google.ads.interactivemedia.v3.api.CompanionAdSlot;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.UiElement;
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
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdPositionType;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_ENDED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_STARTED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_PROGRESS;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.PAUSED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.RESUMED;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAExoPlugin extends PKPlugin implements AdsProvider , com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener {
    private static final PKLog log = PKLog.get("IMAExoPlugin");


    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }

    /////////////////////
    private Player player;
    private Context context;
    private AdInfo adInfo;
    private IMAConfig adConfig;
    private PKMediaConfig mediaConfig;
    //////////////////////


    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;


    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager adsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory sdkFactory;

    // Ad-enabled video player.
    private ExoPlayerWithAdPlayback videoPlayerWithAdPlayback;

    // Button the user taps to begin video playback and ad request.
    private View mPlayButton;

    // VAST ad tag URL to use when requesting ads during video playback.
    private String mCurrentAdTagUrl;

    // URL of content video.
    private String mContentVideoUrl;

    // ViewGroup to render an associated companion ad into.
    private ViewGroup mCompanionViewGroup;

    // Tracks if the SDK is playing an ad, since the SDK might not necessarily use the video
    // player provided to play the video ad.
    private boolean mIsAdPlaying;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader adsLoader;
    private AdsLoader.AdsLoadedListener adsLoadedListener;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private ImaSdkSettings imaSdkSettings;
    private AdsRenderingSettings renderingSettings;
    private AdCuePoints adTagCuePoints;
    private boolean isAdDisplayed;
    private boolean isAdIsPaused;
    private boolean isAdRequested;
    private boolean isInitWaiting;
    private boolean isAllAdsCompleted;
    private boolean isAdError;
    private boolean adPlaybackCancelled;
    private boolean appIsInBackground;
    private boolean isContentEndedBeforeMidroll;

    private boolean isContentPrepared;
    private boolean isAutoPlay;
    private boolean appInBackgroundDuringAdLoad;
    private PlayerEvent.Type lastPlaybackPlayerState;
    private com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType lastAdEventReceived;
    private boolean adManagerInitDuringBackground;
    private PKAdProviderListener pkAdProviderListener;
    ////////////////////
    private MessageBus messageBus;


    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "IMAExo";
        }

        @Override
        public PKPlugin newInstance() {
            return new IMAExoPlugin();
        }

        @Override
        public void warmUp(Context context) {
            log.d("warmUp started");
            ImaSdkFactory.getInstance().createAdsLoader(context);
        }
    };

    ////////PKPlugin

    ///////////END PKPlugin
    @Override
    protected void onLoad(final Player player, Object config, final MessageBus messageBus, Context context) {
        this.player = player;
        if (player == null) {
            log.e("Error, player instance is null.");
            return;
        }
        videoPlayerWithAdPlayback = new ExoPlayerWithAdPlayback(context);
        //videoPlayerWithAdPlayback.setId(new Integer(123456789));

        player.getView().addView(videoPlayerWithAdPlayback.getExoPlayerView());
        this.context = context;
        this.messageBus = messageBus;
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Received:PlayerEvent:" + event.eventType().name() + " lastAdEventReceived = " + lastAdEventReceived);
                AdCuePoints adCuePoints = new AdCuePoints(getAdCuePoints());
                if (event.eventType() == PlayerEvent.Type.ENDED) {
                    if (!isContentPrepared) {
                        log.d("Event: ENDED ignored content is not prepared");
                        return;
                    }
                    lastPlaybackPlayerState = PlayerEvent.Type.ENDED;
                    if (adInfo != null) {
                        log.d("Event: ENDED adInfo.getAdIndexInPod() = " + adInfo.getAdIndexInPod() + " -  adInfo.getTotalAdsInPod() = " + adInfo.getTotalAdsInPod());
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


        adConfig = parseConfig(config);
    }

    private static IMAConfig parseConfig(Object config) {
        if (config instanceof IMAConfig) {
            return ((IMAConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), IMAConfig.class);
        }
        return null;
    }


    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("Start onUpdateMedia");
        this.mediaConfig = mediaConfig;
        log.d("mediaConfig start pos  = " + mediaConfig.getStartPosition());
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
        videoPlayerWithAdPlayback.setContentProgressProvider(player);
        clearAdsLoader();
        imaSetup();
        log.d("adtag = " + adConfig.getAdTagURL());

        requestAdsFromIMA(adConfig.getAdTagURL());
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

    ////////Ads Plugin
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
        }
    }

    private AdsRenderingSettings getRenderingSettings() {

        renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();

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
        return renderingSettings;
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
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");
        appIsInBackground = true;
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
        pause();
    }

    @Override
    protected void onApplicationResumed() {
        log.d("onApplicationResumed");
        appIsInBackground = false;
        if (isAdDisplayed) {
            videoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
        } else if (player != null && lastPlaybackPlayerState == PlayerEvent.Type.PLAYING) {
            player.play();
        } else {
            log.d("onApplicationResumed Default..... lastAdEventReceived = " + lastAdEventReceived);

        }
    }

    @Override
    protected void onDestroy() {
        log.d("IMA Start onDestroy");

        resetIMA();
        if (adsLoader != null) {
            adsLoader.removeAdsLoadedListener(adsLoadedListener);
            adsLoadedListener = null;
            adsLoader = null;
        }

    }

    protected void resetIMA() {
        log.d("Start resetIMA");
        isAdError = false;
        isAdRequested = false;
        isAdDisplayed = false;
        lastPlaybackPlayerState = null;
        lastAdEventReceived = null;

//        cancelAdDisplayedCheckTimer();
//        cancelAdManagerTimer();

        adTagCuePoints = null;
        adPlaybackCancelled = false;
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
    }


    private void requestAdsFromIMA(String adTagUrl) {

        if (TextUtils.isEmpty(adTagUrl)) {
            log.d("AdTag is empty avoiding ad request");
            isAdRequested = true;
            displayContent();
            preparePlayer(false);
            return;
        }
        resetIMA();

        log.d("Do requestAdsFromIMA");
        mAdDisplayContainer = sdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(videoPlayerWithAdPlayback.getVideoAdPlayer());
        mAdDisplayContainer.setAdContainer(videoPlayerWithAdPlayback.getAdUiContainer());

        // Set up spots for companions.

        ViewGroup adCompanionViewGroup = null;
        if (adCompanionViewGroup != null) {
            CompanionAdSlot companionAdSlot = sdkFactory.createCompanionAdSlot();
            companionAdSlot.setContainer(adCompanionViewGroup);
            companionAdSlot.setSize(728, 90);
            ArrayList<CompanionAdSlot> companionAdSlots = new ArrayList<CompanionAdSlot>();
            companionAdSlots.add(companionAdSlot);
            mAdDisplayContainer.setCompanionSlots(companionAdSlots);
        }

        // Create the ads request.
        final AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        if (adConfig.getAdLoadTimeOut() > 0 && adConfig.getAdLoadTimeOut() < Consts.MILLISECONDS_MULTIPLIER && adConfig.getAdLoadTimeOut() != IMAConfig.DEFAULT_AD_LOAD_TIMEOUT) {
            request.setVastLoadTimeout(adConfig.getAdLoadTimeOut() * Consts.MILLISECONDS_MULTIPLIER);
        }
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(videoPlayerWithAdPlayback.getContentProgressProvider());
        messageBus.post(new AdEvent.AdRequestedEvent(adTagUrl));
        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

    @Override
    public void start() {
        isAutoPlay = true; // start will be called only on first time media is played programmatically
        isAdRequested = true;
        if (adsManager != null) {
            log.d("IMA adsManager.init called");
            if (appIsInBackground) {
                log.d("Start: Ad Manager Init : " + adManagerInitDuringBackground);
                adManagerInitDuringBackground = true;
            } else {
                adsManager.init(getRenderingSettings());
            }
        } else {
            isInitWaiting = true;
        }
        videoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
    }

    @Override
    public void destroyAdsManager() {
        if (adsManager == null) {
            return;
        }
        log.d("IMA Start destroyAdsManager");
        videoPlayerWithAdPlayback.stop();
        contentCompleted();
        adsManager.destroy();
        adsManager = null;
        isAdRequested = false;
        isAdDisplayed = false;
        adPlaybackCancelled = false;
    }

    @Override
    public void resume() {
        log.d("AD Event resume mIsAdDisplayed = " + isAdDisplayed);
        if (isAdDisplayed) {
            videoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
        }
    }

    @Override
    public void pause() {
        log.d("AD Event pause mIsAdDisplayed = " + isAdDisplayed);
        if (isAdDisplayed) {
            videoPlayerWithAdPlayback.getVideoAdPlayer().pauseAd();
        }
        if (player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    public void contentCompleted() {
        if (adsLoader != null) {
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
    public boolean isAdPaused() {
        log.d("isAdPaused: " + isAdIsPaused);
        return  isAdIsPaused;
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
    public boolean isAdError() {
        return isAdError;
    }

    @Override
    public long getDuration() {
        long duration = (long) Math.ceil(videoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getDuration());
        log.d("XXXXXXXXXX getDuration: " + duration);
        return duration;
    }

    @Override
    public long getCurrentPosition() {
        long currPos = (long) Math.ceil(videoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getCurrentTime());
        //log.d("getCurrentPosition: " + currPos);
        messageBus.post(new AdEvent.AdPlayHeadEvent(currPos));
        return currPos;
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

    private void sendCuePointsUpdate() {
        List<Long> cuePoints = getAdCuePoints();
        if (cuePoints.size() > 0) {
            messageBus.post(new AdEvent.AdCuePointsUpdateEvent(new AdCuePoints(cuePoints)));
        }
    }

    private List<Long> getAdCuePoints() {
        List<Long> adCuePoints = new ArrayList<>();
        if (adsManager != null && adsManager.getAdCuePoints() != null) {
            for (Float cuePoint : adsManager.getAdCuePoints()) {
                if (cuePoint >= 0) {
                    adCuePoints.add(cuePoint.longValue() * 1000);
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
        log.d("Event: onAdError" + adErrorEvent.getError().getErrorCode());
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
        displayContent();
        preparePlayer(isAutoPlay);
    }

    private void displayAd() {
        log.d("ZZZ  displayAd");
        videoPlayerWithAdPlayback.getExoPlayerView().setVisibility(View.VISIBLE);
        player.getView().hideVideoSurface();
    }

    private void displayContent() {
        log.d("ZZZ displayContent");
        videoPlayerWithAdPlayback.getExoPlayerView().setVisibility(View.GONE);
        player.getView().showVideoSurface();
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
                        log.d("Event preparePlayer true");
                        if (player != null && player.getView() != null && !isAdDisplayed()) {
                            displayContent();
                            player.play();
                        }

                        messageBus.remove(this);
                    }
                }, PlayerEvent.Type.DURATION_CHANGE);
            }
        }
    }

    private void resetFlagsOnError() {
        isAdError = true;
        adPlaybackCancelled = true;
        isAdRequested = true;
        isAdDisplayed = false;
        //cancelAdDisplayedCheckTimer();
        //cancelAdManagerTimer();
    }

    private void sendError(Enum errorType, String message, Throwable exception) {
        log.e("Ad Error: " + errorType.name() + " with message " + message);
        AdEvent errorEvent = new AdEvent.Error(new PKError(errorType, message, exception));
        messageBus.post(errorEvent);
    }

    @Override
    public void onAdEvent(com.google.ads.interactivemedia.v3.api.AdEvent adEvent) {
        lastAdEventReceived = adEvent.getType();
        log.d("ZZZ Event Name: " + lastAdEventReceived.name());


        if (adEvent.getAdData() != null) {
            log.i("Event Data: " + adEvent.getAdData().toString());
        }

        if (adsManager == null) {
            return;
        }

        switch (lastAdEventReceived) {
            case LOADED:
                adInfo = createAdInfo(adEvent.getAd());
                if (appIsInBackground) {
                    appInBackgroundDuringAdLoad = true;
                    if (adsManager != null) {
                        log.d("LOADED call   adsManager.pause()");
                        adsManager.pause();
                    }
                } else {
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
                log.d("CONTENT_PAUSE_REQUESTED appIsInBackground = " + appIsInBackground);
                if (appIsInBackground) {
                    appInBackgroundDuringAdLoad = true;
                    if (adsManager != null) {
                        adsManager.pause();
                    }
                }
                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
                displayAd();
                isAdDisplayed = true;
                if (player != null) {
                    player.pause();
                }
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
                videoPlayerWithAdPlayback.resumeContentAfterAdPlayback();
                if (!isContentPrepared) {
                    log.d("Content not prepared.. Preparing and calling play.");
                    if (pkAdProviderListener != null && !appIsInBackground) {
                        log.d("preparePlayer and play");
                        displayContent();
                        preparePlayer(true);
                    }
                } else if (player != null) {
                    displayContent();
                    long duration = player.getDuration();
                    long position = player.getCurrentPosition();
                    log.d("Content prepared.. lastPlaybackPlayerState = " + lastPlaybackPlayerState + ", time = " + position + "/" + duration);
                    if (lastPlaybackPlayerState != PlayerEvent.Type.ENDED && (duration < 0 || position <= duration)) {
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
                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                displayContent();
                if (adsManager != null) {
                    log.d("AD_ALL_ADS_COMPLETED onDestroy");
                    onDestroy();
                }

                break;
            case STARTED:
                log.d("AD STARTED");
                isAdDisplayed = true;
                isAdIsPaused = false;
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
                isAdIsPaused = false;
                adInfo.setAdPlayHead(getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER);
                messageBus.post(new AdEvent.AdResumedEvent(adInfo));
                break;
            case COMPLETED:
                log.d("AD COMPLETED");
                messageBus.post(new AdEvent(AdEvent.Type.COMPLETED));
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
                //preparePlayer(false);
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
                adsManager.start();
                messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_READY));
                break;
            case AD_PROGRESS:
                messageBus.post(new AdEvent(AD_PROGRESS));
                break;
            case AD_BREAK_STARTED:
                messageBus.post(new AdEvent(AD_BREAK_STARTED));
                break;
            case  AD_BREAK_ENDED:
                messageBus.post(new AdEvent(AD_BREAK_ENDED));
                break;
            case  CUEPOINTS_CHANGED:
                sendCuePointsUpdate();
                break;
            case LOG:
                isAdRequested = true;
                //for this case no AD ERROR is fired need to show view {type=adLoadError, errorCode=1009, errorMessage=The response does not contain any valid ads.}
                preparePlayer(false);
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
            default:
                break;
        }
    }

    private void sendCuePointsUpdateEvent() {
        adTagCuePoints = new AdCuePoints(getAdCuePoints());
        videoPlayerWithAdPlayback.setAdCuePoints(adTagCuePoints);
        messageBus.post(new AdEvent.AdCuePointsUpdateEvent(adTagCuePoints));
    }

    private AdsLoader.AdsLoadedListener getAdsLoadedListener() {
        if (adsLoadedListener != null) {
            return adsLoadedListener;
        }
        adsLoadedListener = new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                adsManager = adsManagerLoadedEvent.getAdsManager();

                //Attach event and error event listeners.

                adsManager.addAdErrorListener(IMAExoPlugin.this);
                adsManager.addAdEventListener(IMAExoPlugin.this);
                sendCuePointsUpdateEvent();

                if (isInitWaiting) {
                    adsManager.init(getRenderingSettings());
                    sendCuePointsUpdate();
                    isInitWaiting = false;
                }
            }
        };
        return adsLoadedListener;
    }

}