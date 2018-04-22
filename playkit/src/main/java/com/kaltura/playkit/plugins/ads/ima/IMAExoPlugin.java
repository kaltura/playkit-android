package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
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
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
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
    private boolean isContentPrepared;
    private boolean isAutoPlay;
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
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
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
                log.d("onLoad:PlayerEvent:" + event);
                if (event.eventType() == PlayerEvent.Type.ENDED) {
                    contentCompleted();
                }
            }
        }, PlayerEvent.Type.ENDED);

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
        //isContentPrepared = false;
        isAutoPlay = false;
        isAdRequested = false;
        isAdDisplayed = false;
        isAllAdsCompleted = false;
        //isContentEndedBeforeMidroll = false;
        //lastEventReceived = null;
        if (adsManager != null) {
            adsManager.destroy();
        }
        videoPlayerWithAdPlayback.setContentProgressProvider(player);
        clearAdsLoader();
        imaSetup();
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

            renderingSettings = sdkFactory.createAdsRenderingSettings();
            if (mediaConfig != null && mediaConfig.getStartPosition() > 0) {
                renderingSettings.setPlayAdsAfterTime(mediaConfig.getStartPosition());
            }

            if (adConfig.getVideoMimeTypes() != null && adConfig.getVideoMimeTypes().size() > 0) {
                renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
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
        imaSdkSettings.setLanguage(adConfig.getLanguage());
        imaSdkSettings.setDebugMode(adConfig.isDebugMode());
    }



    @Override
    protected void onApplicationPaused() {
        pause();
    }

    @Override
    protected void onApplicationResumed() {

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

        //cancelAdDisplayedCheckTimer();
        //cancelAdManagerTimer();

        adTagCuePoints = null;
        adPlaybackCancelled = false;
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
    }


    private void requestAdsFromIMA(String adTagUrl) {
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
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(videoPlayerWithAdPlayback.getContentProgressProvider());

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
                adsManager.init(renderingSettings);
            }
        } else {
            isInitWaiting = true;
        }
        videoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
    }

    @Override
    public void destroyAdsManager() {

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
    }

    @Override
    public void contentCompleted() {
        adsLoader.contentComplete();
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
        long duration = (long) videoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getDuration();
        log.d("xxx getDuration: " + duration);
        return duration;
    }

    @Override
    public long getCurrentPosition() {
        long currPos = (long) Math.ceil(videoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getCurrentTime());
        log.d("xxx getCurrentPosition: " + currPos);
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
        //player.getView().removeView(player.getView().findViewById(new Integer(123456789)));
        player.getView().showVideoSurface();
        preparePlayer(isAutoPlay);
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
                        if (player != null && player.getView() != null && !isAdDisplayed()) {
                            player.getView().showVideoSurface();
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
        log.i("Event: " + adEvent.getType());
        if (adEvent.getAdData() != null) {
            log.i("Event: " + adEvent.getAdData().toString());
        }

        if (adsManager == null) {
            return;
        }
        switch (adEvent.getType()) {

            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                messageBus.post(new AdEvent(AdEvent.Type.LOADED));
                adsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                log.d("AD_CONTENT_PAUSE_REQUESTED");
                videoPlayerWithAdPlayback.getExoPlayerView().setVisibility(View.VISIBLE);
                player.getView().hideVideoSurface();
                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
                isAdDisplayed = true;
                if (player != null) {
                    player.pause();
                }
                if (adEvent.getAd().getAdPodInfo().getTotalAds() > 1) {
                    player.getView().hideVideoSurface();
                }
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED");
                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
                isAdDisplayed = false;
                if (player != null && player.getCurrentPosition() < player.getDuration()) {
                    //player.getView().removeView(player.getView().findViewById(new Integer(123456789)));
                    videoPlayerWithAdPlayback.getExoPlayerView().setVisibility(View.GONE);
                    player.getView().showVideoSurface();
                    player.play();
                }
                break;
            case ALL_ADS_COMPLETED:
                log.d("AD_ALL_ADS_COMPLETED");
                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                videoPlayerWithAdPlayback.getExoPlayerView().setVisibility(View.GONE);
                player.getView().showVideoSurface();
                if (adsManager != null) {
                    log.d("AD_ALL_ADS_COMPLETED onDestroy");
                    onDestroy();
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
                messageBus.post(new AdEvent(PAUSED));
                break;
            case RESUMED:
                log.d("AD RESUMED");
                isAdIsPaused = false;
                messageBus.post(new AdEvent(RESUMED));
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
                preparePlayer(false);
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
            default:
                break;
        }
    }

    private void sendCuePointsUpdateEvent() {
        adTagCuePoints = new AdCuePoints(getAdCuePoints());
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

                renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
                if (adConfig.getVideoMimeTypes().size() > 0) {
                    renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
                }

                //if both are false we remove the support int ad count down in ad
                if (!adConfig.getAdAttribution() && !adConfig.getAdCountDown()) {
                    renderingSettings.setUiElements(Collections.<UiElement>emptySet());
                }

                if (isInitWaiting) {
                    adsManager.init(renderingSettings);
                    sendCuePointsUpdate();
                    isInitWaiting = false;
                }

            }
        };
        return adsLoadedListener;
    }

}





//package com.kaltura.playkit.plugins.ads.ima;
//
//import android.content.Context;
//import android.view.View;
//import android.view.ViewGroup;
//
//import com.google.ads.interactivemedia.v3.api.Ad;
//import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
//import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
//import com.google.ads.interactivemedia.v3.api.AdsLoader;
//import com.google.ads.interactivemedia.v3.api.AdsManager;
//import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
//import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
//import com.google.ads.interactivemedia.v3.api.AdsRequest;
//import com.google.ads.interactivemedia.v3.api.CompanionAdSlot;
//import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
//import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
//import com.google.ads.interactivemedia.v3.api.UiElement;
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import com.kaltura.playkit.MessageBus;
//import com.kaltura.playkit.PKEvent;
//import com.kaltura.playkit.PKLog;
//import com.kaltura.playkit.PKMediaConfig;
//import com.kaltura.playkit.PKPlugin;
//import com.kaltura.playkit.Player;
//import com.kaltura.playkit.PlayerDecorator;
//import com.kaltura.playkit.PlayerEvent;
//import com.kaltura.playkit.ads.AdEnabledPlayerController;
//import com.kaltura.playkit.ads.PKAdInfo;
//import com.kaltura.playkit.plugins.ads.AdCuePoints;
//import com.kaltura.playkit.plugins.ads.AdEvent;
//import com.kaltura.playkit.plugins.ads.AdInfo;
//import com.kaltura.playkit.plugins.ads.AdsProvider;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_ENDED;
//import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_STARTED;
//import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_PROGRESS;
//import static com.kaltura.playkit.plugins.ads.AdEvent.Type.PAUSED;
//import static com.kaltura.playkit.plugins.ads.AdEvent.Type.RESUMED;
//
//
///**
// * Created by gilad.nadav on 17/11/2016.
// */
//
//public class IMAExoPlugin extends PKPlugin implements AdsProvider , com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener {
//
//    private static final PKLog log = PKLog.get("IMAExoPlugin");
//
//
//
//    @Override
//    protected PlayerDecorator getPlayerDecorator() {
//        return new AdEnabledPlayerController(this);
//    }
//
//    /////////////////////
//    private Player player;
//    private Context context;
//    private AdInfo adInfo;
//    private IMAConfig adConfig;
//    private PKMediaConfig mediaConfig;
//    //////////////////////
//
//
//    // Container with references to video player and ad UI ViewGroup.
//    private AdDisplayContainer mAdDisplayContainer;
//
//    // The AdsLoader instance exposes the requestAds method.
//    private AdsLoader mAdsLoader;
//
//    // AdsManager exposes methods to control ad playback and listen to ad events.
//    private AdsManager mAdsManager;
//
//    // Factory class for creating SDK objects.
//    private ImaSdkFactory mSdkFactory;
//
//    // Ad-enabled video player.
//    private ExoPlayerWithAdPlayback mVideoPlayerWithAdPlayback;
//
//    // Button the user taps to begin video playback and ad request.
//    private View mPlayButton;
//
//    // VAST ad tag URL to use when requesting ads during video playback.
//    private String mCurrentAdTagUrl;
//
//    // URL of content video.
//    private String mContentVideoUrl;
//
//    // ViewGroup to render an associated companion ad into.
//    private ViewGroup mCompanionViewGroup;
//
//    // Tracks if the SDK is playing an ad, since the SDK might not necessarily use the video
//    // player provided to play the video ad.
//    private boolean mIsAdPlaying;
//
//    // The AdsLoader instance exposes the requestAds method.
//    private AdsLoader adsLoader;
//    private AdsLoader.AdsLoadedListener adsLoadedListener;
//
//    // AdsManager exposes methods to control ad playback and listen to ad events.
//    private ImaSdkFactory sdkFactory;
//    private AdsManager adsManager;
//    private ImaSdkSettings imaSdkSettings;
//    private AdsRenderingSettings renderingSettings;
//    // Whether an ad is displayed.
//    private boolean isAdDisplayed;
//    private boolean isAdIsPaused;
//    private boolean isAdRequested;
//    private boolean isInitWaiting;
//    private boolean isAllAdsCompleted;
//    ////////////////////
//    private MessageBus messageBus;
//
//
//    public static final Factory factory = new Factory() {
//        @Override
//        public String getName() {
//            return "IMAAdvanced";
//        }
//
//        @Override
//        public PKPlugin newInstance() {
//            return new IMAExoPlugin();
//        }
//
//        @Override
//        public void warmUp(Context context) {
//            log.d("warmUp started");
//            ImaSdkFactory.getInstance().createAdsLoader(context);
//        }
//    };
//
//    ////////PKPlugin
//
//    ///////////END PKPlugin
//
//
//    @Override
//    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
//        this.player = player;
//        this.context = context;
//        this.isAllAdsCompleted = false;
//
//        if (this.messageBus == null) {
//            this.messageBus = messageBus;
//            this.messageBus.listen(new PKEvent.Listener() {
//                @Override
//                public void onEvent(PKEvent event) {
//                    log.d("Received:PlayerEvent:" + event.eventType().name());
//                    AdCuePoints adCuePoints = new AdCuePoints(getAdCuePoints());
//                    if (event.eventType() == PlayerEvent.Type.ENDED) {
//                        if (isAllAdsCompleted || !adCuePoints.hasPostRoll() || adInfo == null || (adInfo.getAdIndexInPod() == adInfo.getTotalAdsInPod())) {
//                            log.d("contentCompleted on ended");
//                            contentCompleted();
//                        } else {
//                            log.d("contentCompleted delayed");
//                            //isContentEndedBeforeMidroll = true;
//                        }
//                    }
//                }
//            }, PlayerEvent.Type.ENDED);
//        }
//        mVideoPlayerWithAdPlayback = new ExoPlayerWithAdPlayback(context);
//        player.getView().addView(mVideoPlayerWithAdPlayback.getExoPlayerView());
//        adConfig = parseConfig(config);
//    }
//
//    private static IMAConfig parseConfig(Object config) {
//        if (config instanceof IMAConfig) {
//            return ((IMAConfig) config);
//
//        } else if (config instanceof JsonObject) {
//            return new Gson().fromJson(((JsonObject) config), IMAConfig.class);
//        }
//        return null;
//    }
//
//    @Override
//    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
//        log.d("Start onUpdateMedia");
//        this.mediaConfig = mediaConfig;
//        //isContentPrepared = false;
//        //isAutoPlay = false;
//        isAdRequested = false;
//        isAdDisplayed = false;
//        isAllAdsCompleted = false;
//        //isContentEndedBeforeMidroll = false;
//        //lastEventReceived = null;
//        if (adsManager != null) {
//            adsManager.destroy();
//        }
//        clearAdsLoader();
//        imaSetup();
//        requestAdsFromIMA(adConfig.getAdTagURL());
//    }
//
//    private void clearAdsLoader() {
//        if (adsLoader != null) {
//            adsLoader.removeAdErrorListener(this);
//            adsLoader.removeAdsLoadedListener(adsLoadedListener);
//            adsLoadedListener = null;
//            adsLoader = null;
//        }
//    }
//
//    @Override
//    protected void onUpdateConfig(Object config) {
//
//    }
//
//    @Override
//    protected void onApplicationPaused() {
//        pause();
//    }
//
//    @Override
//    protected void onApplicationResumed() {
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        log.d("IMA Start onDestroy");
//
//        if (mAdsManager != null) {
//            mAdsManager.destroy();
//            mAdsManager = null;
//        }
//        if (adsLoader != null) {
//            adsLoader.removeAdsLoadedListener(adsLoadedListener);
//            adsLoadedListener = null;
//            adsLoader = null;
//        }
//
//    }
//
//
//    ////////Ads Plugin
//    private void imaSetup() {
//        log.d("imaSetup start");
//        imaSettingSetup();
//        if (sdkFactory == null) {
//            sdkFactory = ImaSdkFactory.getInstance();
//        }
//        if (adsLoader == null) {
//            adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings);
//            // Add listeners for when ads are loaded and for errors.
//            adsLoader.addAdErrorListener(this);
//            adsLoader.addAdsLoadedListener(getAdsLoadedListener());
//
//            renderingSettings = sdkFactory.createAdsRenderingSettings();
//            if (mediaConfig != null && mediaConfig.getStartPosition() > 0) {
//                renderingSettings.setPlayAdsAfterTime(mediaConfig.getStartPosition());
//            }
//
//            if (adConfig.getVideoMimeTypes() != null && adConfig.getVideoMimeTypes().size() > 0) {
//                renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
//            }
//
//            //if both are false we remove the support int ad count down in ad
//            if (!adConfig.getAdAttribution() && !adConfig.getAdCountDown()) {
//                renderingSettings.setUiElements(Collections.<UiElement>emptySet());
//            }
//
//            if (adConfig.getVideoBitrate() != -1) {
//                renderingSettings.setBitrateKbps(adConfig.getVideoBitrate());
//            }
//        }
//    }
//
//    private void imaSettingSetup() {
//        if (imaSdkSettings == null) {
//            imaSdkSettings = ImaSdkFactory.getInstance().createImaSdkSettings();
//        }
//        // Tell the SDK we want to control ad break playback.
//        //imaSdkSettings.setAutoPlayAdBreaks(true);
//        imaSdkSettings.setLanguage(adConfig.getLanguage());
//        imaSdkSettings.setDebugMode(adConfig.isDebugMode());
//    }
//
//    private void requestAdsFromIMA(String adTagUrl) {
//        log.d("Do requestAdsFromIMA");
//        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
//        mAdDisplayContainer.setPlayer(mVideoPlayerWithAdPlayback.getVideoAdPlayer());
//        mAdDisplayContainer.setAdContainer(mVideoPlayerWithAdPlayback.getAdUiContainer());
//
//        // Set up spots for companions.
//
//        ViewGroup adCompanionViewGroup = null;
//        if (adCompanionViewGroup != null) {
//            CompanionAdSlot companionAdSlot = mSdkFactory.createCompanionAdSlot();
//            companionAdSlot.setContainer(adCompanionViewGroup);
//            companionAdSlot.setSize(728, 90);
//            ArrayList<CompanionAdSlot> companionAdSlots = new ArrayList<CompanionAdSlot>();
//            companionAdSlots.add(companionAdSlot);
//            mAdDisplayContainer.setCompanionSlots(companionAdSlots);
//        }
//
//
//        // Create the ads request.
//        final AdsRequest request = mSdkFactory.createAdsRequest();
//        request.setAdTagUrl(adTagUrl);
//        request.setAdDisplayContainer(mAdDisplayContainer);
//        request.setContentProgressProvider(mVideoPlayerWithAdPlayback.getContentProgressProvider());
//
//        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
//        adsLoader.requestAds(request);
//    }
//
//
//    @Override
//    public void start() {
//        if (mAdsManager != null) {
//            mAdsManager.start();
//        }
////        isAutoPlay = true; // start will be called only on first time media is played programmatically
////        isAdRequested = true;
////        if (adsManager != null) {
////            log.d("IMA adsManager.init called");
////            if (appIsInBackground) {
////                log.d("Start: Ad Manager Init : " + adManagerInitDuringBackground);
////                adManagerInitDuringBackground = true;
////            } else {
////                adsManager.init(renderingSettings);
////            }
////        } else {
////            isInitWaiting = true;
////        }
//    }
//
//    @Override
//    public void destroyAdsManager() {
//        if (mAdsManager != null) {
//            mAdsManager.destroy();
//        }
//    }
//
//    @Override
//    public void resume() {
//        log.d("AD Event resume mIsAdDisplayed = " + isAdDisplayed);
//        if (isAdDisplayed) {
//            mVideoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
//        }
//    }
//
//    @Override
//    public void pause() {
//        log.d("AD Event pause mIsAdDisplayed = " + isAdDisplayed);
//        if (isAdDisplayed) {
//            mVideoPlayerWithAdPlayback.getVideoAdPlayer().pauseAd();
//        }
//    }
//
//    @Override
//    public void contentCompleted() {
//        log.d("contentCompleted");
//        if (adsManager != null) {
//            adsLoader.contentComplete();
//        }
//    }
//
//    @Override
//    public PKAdInfo getAdInfo() {
//        return adInfo;
//    }
//
//    @Override
//    public boolean isAdDisplayed() {
//        //log.d("isAdDisplayed: " + mIsAdDisplayed);
//        return isAdDisplayed;
//    }
//
//    @Override
//    public boolean isAdPaused() {
//        log.d("isAdPaused: " + isAdIsPaused);
//        return  isAdIsPaused;
//    }
//
//    @Override
//    public boolean isAdRequested() {
//        log.d("isAdRequested: " + isAdRequested);
//        return isAdRequested;
//    }
//
//    @Override
//    public boolean isAllAdsCompleted() {
//        return false;
//    }
//
//    @Override
//    public boolean isAdError() {
//        return false;
//    }
//
//    @Override
//    public long getDuration() {
//        return new Float(mVideoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getDuration()).longValue();
//    }
//
//    @Override
//    public long getCurrentPosition() {
//        return new Float(mVideoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getCurrentTime()).longValue();
//    }
//
//    @Override
//    public void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController) {
//
//    }
//
//    @Override
//    public void removeAdProviderListener() {
//
//    }
//
//    @Override
//    public void skipAd() {
//        if (mAdsManager != null) {
//            mAdsManager.skip();
//        }
//    }
//
//
//
//    private void sendCuePointsUpdate() {
//        List<Long> cuePoints = getAdCuePoints();
//        if (cuePoints.size() > 0) {
//            messageBus.post(new AdEvent.AdCuePointsUpdateEvent(new AdCuePoints(cuePoints)));
//        }
//    }
//
//    private List<Long> getAdCuePoints() {
//        List<Long> adCuePoints = new ArrayList<>();
//        if (mAdsManager != null && mAdsManager.getAdCuePoints() != null) {
//            for (Float cuePoint : mAdsManager.getAdCuePoints()) {
//                if (cuePoint >= 0) {
//                    adCuePoints.add(cuePoint.longValue() * 1000);
//                } else {
//                    adCuePoints.add(cuePoint.longValue());
//                }
//            }
//        }
//        return adCuePoints;
//    }
//
//    private AdInfo createAdInfo(Ad ad) {
//
//        return adInfo;
//
//    }
//
//    @Override
//    public void onAdError(AdErrorEvent adErrorEvent) {
//
//
//    }
//
//    @Override
//    public void onAdEvent(com.google.ads.interactivemedia.v3.api.AdEvent adEvent) {
//        log.i("Event: " + adEvent.getType());
//        if (adEvent.getAdData() != null) {
//            log.i("Event: " + adEvent.getAdData().toString());
//        }
//
//        if (mAdsManager == null) {
//            return;
//        }
//        switch (adEvent.getType()) {
//
//            case LOADED:
//                // AdEventType.LOADED will be fired when ads are ready to be played.
//                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
//                // ad rules playlists, as the SDK will automatically start executing the
//                // playlist.
//                messageBus.post(new AdEvent(AdEvent.Type.LOADED));
//                mAdsManager.start();
//                break;
//            case CONTENT_PAUSE_REQUESTED:
//                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
//                // ad is played.
//                log.d("AD_CONTENT_PAUSE_REQUESTED");
//                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
//                isAdDisplayed = true;
//                if (player != null) {
//                    player.pause();
//                }
//                if (adEvent.getAd().getAdPodInfo().getTotalAds() > 1) {
//                    player.getView().hideVideoSurface();
//                }
//                break;
//            case CONTENT_RESUME_REQUESTED:
//                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
//                // and you should start playing your content.
//                log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED");
//                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
//                isAdDisplayed = false;
//                if (player != null && player.getCurrentPosition() < player.getDuration()) {
//                    player.getView().removeView(player.getView().findViewById(new Integer(123456789)));
//                    player.getView().showVideoSurface();
//                    player.play();
//                }
//                break;
//            case ALL_ADS_COMPLETED:
//                log.d("AD_ALL_ADS_COMPLETED");
//                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
//                player.getView().showVideoSurface();
//                if (mAdsManager != null) {
//                    log.d("AD_ALL_ADS_COMPLETED onDestroy");
//                    onDestroy();
//                }
//
//                break;
//            case STARTED:
//                log.d("AD STARTED");
//                isAdIsPaused = false;
//                adInfo = createAdInfo(adEvent.getAd());
//                messageBus.post(new AdEvent.AdStartedEvent(adInfo));
//                break;
//            case PAUSED:
//                log.d("AD PAUSED");
//                isAdIsPaused = true;
//                messageBus.post(new AdEvent(PAUSED));
//                break;
//            case RESUMED:
//                log.d("AD RESUMED");
//                isAdIsPaused = false;
//                messageBus.post(new AdEvent(RESUMED));
//                break;
//            case COMPLETED:
//                log.d("AD COMPLETED");
//                messageBus.post(new AdEvent(AdEvent.Type.COMPLETED));
//                break;
//            case FIRST_QUARTILE:
//                messageBus.post(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
//                break;
//            case MIDPOINT:
//                messageBus.post(new AdEvent(AdEvent.Type.MIDPOINT));
//                break;
//            case THIRD_QUARTILE:
//                messageBus.post(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
//                break;
//            case SKIPPED:
//                messageBus.post(new AdEvent(AdEvent.Type.SKIPPED));
//                break;
//            case CLICKED:
//                isAdIsPaused = true;
//                messageBus.post(new AdEvent(AdEvent.Type.CLICKED));
//                break;
//            case TAPPED:
//                messageBus.post(new AdEvent(AdEvent.Type.TAPPED));
//                break;
//            case ICON_TAPPED:
//                messageBus.post(new AdEvent(AdEvent.Type.ICON_TAPPED));
//                break;
//            case AD_BREAK_READY:
//                mAdsManager.start();
//                messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_READY));
//                break;
//            case AD_PROGRESS:
//                messageBus.post(new AdEvent(AD_PROGRESS));
//                break;
//            case AD_BREAK_STARTED:
//                messageBus.post(new AdEvent(AD_BREAK_STARTED));
//                break;
//            case  AD_BREAK_ENDED:
//                messageBus.post(new AdEvent(AD_BREAK_ENDED));
//                break;
//            case  CUEPOINTS_CHANGED:
//                sendCuePointsUpdate();
//                break;
//            default:
//                break;
//        }
//    }
//
//
//    private AdsLoader.AdsLoadedListener getAdsLoadedListener() {
//        if (adsLoadedListener != null) {
//            return adsLoadedListener;
//        }
//        adsLoadedListener = new AdsLoader.AdsLoadedListener() {
//            @Override
//            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
//                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
//                // events for ad playback and errors.
//                mAdsManager = adsManagerLoadedEvent.getAdsManager();
//
//                //Attach event and error event listeners.
//
//                mAdsManager.addAdErrorListener(IMAExoPlugin.this);
//                mAdsManager.addAdEventListener(IMAExoPlugin.this);
//
//                renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
//                if (adConfig.getVideoMimeTypes().size() > 0) {
//                    renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
//                }
//
//                //if both are false we remove the support int ad count down in ad
//                if (!adConfig.getAdAttribution() && !adConfig.getAdCountDown()) {
//                    renderingSettings.setUiElements(Collections.<UiElement>emptySet());
//                }
//
//                if (isInitWaiting) {
//                    mAdsManager.init(renderingSettings);
//                    sendCuePointsUpdate();
//                    isInitWaiting = false;
//                }
//
//            }
//        };
//        return adsLoadedListener;
//    }
//
//}