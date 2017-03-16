package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
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
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.AdResponseType;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdError;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_ENDED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_BREAK_STARTED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_LOAD_TIMEOUT_TIMER_STARTED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.AD_PROGRESS;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.PAUSED;
import static com.kaltura.playkit.plugins.ads.AdEvent.Type.RESUMED;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAPlugin extends PKPlugin implements AdsProvider, com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener  {

    private static final PKLog log = PKLog.get("IMAPlugin");

    /////////////////////
    private Player player;
    private Context context;
    private AdInfo adInfo;
    private IMAConfig adConfig;
    private PKAdProviderListener pkAdProviderListener;
    private PlayerConfig.Media mediaConfig;
    //////////////////////


    /////////////////////

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
    private boolean isAdRequested = false;
    private boolean isInitWaiting = false;
    private boolean appIsInBackground;
    private boolean adManagerInitDuringBackground;
    private boolean applicationInBackgroundDuringLoaded;

    ////////////////////
    private MessageBus messageBus;

    private AdDisplayContainer adDisplayContainer;
    private CountDownTimer adManagerTimer;
    private boolean adPlaybackCancelled;
    private Timer adDisplayedCheckTimer;

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
            AdsLoader adsLoader = ImaSdkFactory.getInstance().createAdsLoader(context);
            adsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
                @Override
                public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                    log.d("warmUp: onAdsManagerLoaded");
                }
            });
        }
    };

    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }
    ////////PKPlugin

    ///////////END PKPlugin
    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.player = player;
        if (player == null) {
            log.e("Error, player instance is null.");
            return;
        }
        this.mediaConfig = mediaConfig;
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
        }, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED);

        //----------------------------//
        Gson gson = new Gson();
        adConfig =  gson.fromJson(pluginConfig, IMAConfig.class);//IMAConfig.fromJsonObject(pluginConfig);

        adUiContainer = player.getView();
        imaSetup();

        requestAdsFromIMA(adConfig.getAdTagURL());
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

            adManagerTimer = new CountDownTimer(getAdsConfig().getAdLoadTimeOut() * Consts.MILLISECONDS_MULTIPLIER, IMAConfig.DEFAULT_AD_LOAD_COUNT_DOWN_TICK) {
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
                        if (pkAdProviderListener != null) {
                            pkAdProviderListener.onAdLoadingFinished();
                        }

                        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_IGNORED));
                        if (isAdRequested) {
                            adPlaybackCancelled = true;
                        }
                    }
                }
            };
            adManagerTimer.start();
        }

        adDisplayContainer = sdkFactory.createAdDisplayContainer();
        adDisplayContainer.setAdContainer(adUiContainer);

        // Set up spots for companions.

        ViewGroup adCompanionViewGroup = null;
        if (adCompanionViewGroup != null) {
            CompanionAdSlot companionAdSlot = sdkFactory.createCompanionAdSlot();
            companionAdSlot.setContainer(adCompanionViewGroup);
            companionAdSlot.setSize(728, 90);
            ArrayList<CompanionAdSlot> companionAdSlots = new ArrayList<CompanionAdSlot>();
            companionAdSlots.add(companionAdSlot);
            adDisplayContainer.setCompanionSlots(companionAdSlots);
        }

        renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
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

    private void imaSettingSetup() {
        if (imaSdkSettings == null) {
            imaSdkSettings = new ImaSdkSettings();
        }
        // Tell the SDK we want to control ad break playback.
        imaSdkSettings.setAutoPlayAdBreaks(true);
        imaSdkSettings.setLanguage(adConfig.getLanguage());
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        log.d("Start onUpdateMedia");
        isAdRequested = false;
        isAdDisplayed = false;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        log.d("Start onUpdateConfig");

        if (key.equals(IMAConfig.AD_TAG_LANGUAGE)) {
            getAdsConfig().setLanguage((String) value);
        } else if (key.equals(IMAConfig.AD_TAG_URL)) {
            getAdsConfig().setAdTagURL((String) value);
            isAdRequested = false;
            isAdDisplayed = false;
            onDestroy();
            onLoad(player, mediaConfig, getAdsConfig().toJSONObject(), messageBus, context);
        } else if (key.equals(IMAConfig.ENABLE_BG_PLAYBACK)) {
            getAdsConfig().setEnableBackgroundPlayback((boolean) value);
        } else if (key.equals(IMAConfig.AUTO_PLAY_AD_BREAK)) {
            getAdsConfig().setAutoPlayAdBreaks((boolean) value);
        } else if (key.equals(IMAConfig.AD_VIDEO_BITRATE)) {
            getAdsConfig().setVideoBitrate((int) value);
        } else if (key.equals(IMAConfig.AD_VIDEO_MIME_TYPES)) {
            getAdsConfig().setVideoMimeTypes((List<String>) value);
        }
    }

    @Override
    protected void onApplicationPaused() {
        appIsInBackground = true;
        pause();
        cancelAdDisplayedCheckTimer();
        log.d("IMA onApplicationPaused");
    }

    @Override
    protected void onApplicationResumed() {
        log.d("IMA onApplicationResumed adManagerInitDuringBackground = " + adManagerInitDuringBackground + " isAdDisplayed = " + isAdDisplayed);
        appIsInBackground = false;
        if (adsManager != null && adManagerInitDuringBackground) {
            player.getView().hideVideoSurface();
            adsManager.init(renderingSettings);
            sendCuePointsUpdate();
            isInitWaiting = false;
            adManagerInitDuringBackground = false;
            return;
        }
        if (adsManager != null) {
            if (applicationInBackgroundDuringLoaded) {
                player.getView().hideVideoSurface();
                applicationInBackgroundDuringLoaded = false;
                adsManager.start();
            } else if (isAdDisplayed) {
                log.d("IMA onApplicationResumed Ad progress: " + adsManager.getAdProgress());
                if (adsManager.getAdProgress().getDuration() - adsManager.getAdProgress().getCurrentTime() < 1) {
                    log.d("IMA onApplicationResumed player play called");
                    adsManager.resume();
                    player.play();
                } else {
                    log.d("IMA onApplicationResumed ad resumed");
                    adsManager.resume();
                }
            }
        }
        initAdDisplayedCheckTimer();
    }

    @Override
    protected void onDestroy() {
        log.d("IMA Start onDestroy");
        cancelAdDisplayedCheckTimer();

        if (adManagerTimer != null) {
            adManagerTimer.cancel();
            adManagerTimer = null;
        }

        adTagCuePoints = null;
        adPlaybackCancelled = false;
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
        if (adsLoader != null) {
            adsLoader.removeAdErrorListener(this);
            adsLoader.removeAdsLoadedListener(adsLoadedListener);
            adsLoadedListener = null;
            adsLoader = null;
        }
        sdkFactory = null;
        imaSdkSettings = null;
        removeAdProviderListener();
    }

    ////////Ads Plugin

    @Override
    public IMAConfig getAdsConfig() {
        return adConfig;
    }

    private AdsLoader.AdsLoadedListener getAdsLoadedListener() {
        if (adsLoadedListener != null) {
            return adsLoadedListener;
        }
        adsLoadedListener = new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {

                log.d("AdsManager loaded");

                adManagerTimer.cancel();

                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                adsManager = adsManagerLoadedEvent.getAdsManager();

                //Attach event and error event listeners.

                adsManager.addAdErrorListener(IMAPlugin.this);
                adsManager.addAdEventListener(IMAPlugin.this);

                if (isInitWaiting) {
                    if (appIsInBackground) {
                        adManagerInitDuringBackground = true;
                        return;
                    }
                    adsManager.init(renderingSettings);
                    isInitWaiting = false;
                }

            }
        };
        return adsLoadedListener;
    }

    @Override
    public void start() {
        log.d("IMA Start  adsManager.init");
        isAdRequested = true;
        if (adsManager != null) {
            if (appIsInBackground) {
                adManagerInitDuringBackground = true;
            } else {
                adsManager.init(renderingSettings);
            }
        } else{
            isInitWaiting = true;
        }
    }

    private void requestAdsFromIMA(String adTagUrl) {
        log.d("Do requestAdsFromIMA");

        // Create the ads request.
        final AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(adDisplayContainer);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (adsManager == null) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                if (isAdDisplayed || player == null || player.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                VideoProgressUpdate videoProgress = new VideoProgressUpdate(player.getCurrentPosition(),
                        player.getDuration());
                return videoProgress;
            }
        });

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
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
        if (adsManager != null && isAdDisplayed) {
            adsManager.pause();
        }
    }

    @Override
    public void contentCompleted() {
        if (adsManager != null) {
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
    public long getDuration() {
        if (adsManager != null) {
            return (long) adsManager.getAdProgress().getDuration();
        } else {
            return  Consts.TIME_UNSET;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (adsManager != null) {
            return (long) adsManager.getAdProgress().getCurrentTime();
        } else {
            return  Consts.POSITION_UNSET;
        }
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
                if (appIsInBackground) {
                    applicationInBackgroundDuringLoaded = true;
                } else {
                    messageBus.post(new AdEvent(AdEvent.Type.LOADED));
                    if (adPlaybackCancelled) {
                        log.d("discarding ad break");
                        adsManager.discardAdBreak();
                    } else {
                        if(!AdResponseType.VMAP.equals(adConfig.getAdResponseType())){
                            adsManager.start();
                        }
                    }
                }
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                log.d("AD_CONTENT_PAUSE_REQUESTED");

                if (!adPlaybackCancelled) {
                    messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));

                    if (player != null) {
                        player.pause();
                    }
                    if (adEvent.getAd().getAdPodInfo().getTotalAds() > 1) {
                        player.getView().hideVideoSurface();
                    }
                }
                initAdDisplayedCheckTimer();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED");
                messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
                isAdDisplayed = false;
                if (player != null) {
                    player.getView().showVideoSurface();
                    long duration = player.getDuration();
                    if (duration < 0 || player.getCurrentPosition() < duration) {
                        player.play();
                    }
                }
                adPlaybackCancelled = false;
                break;
            case ALL_ADS_COMPLETED:
                log.d("AD_ALL_ADS_COMPLETED");
                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
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

                if (adsManager != null && appIsInBackground) {
                    adsManager.pause();
                }

                if (pkAdProviderListener != null) {
                    pkAdProviderListener.onAdLoadingFinished();
                }

                adInfo = createAdInfo(adEvent.getAd());
                messageBus.post(new AdEvent.AdStartedEvent(adInfo));

                if (adTagCuePoints == null) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            log.d("AD CUEPOINTS CHANGED TRIGGERED WITH DELAY");
                            adTagCuePoints = new AdCuePoints(getAdCuePoints());
                            messageBus.post(new AdEvent.AdCuePointsUpdateEvent(adTagCuePoints));
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
                if (player != null && player.getView() != null) {
                    player.getView().hideVideoSurface();
                }
                isAdIsPaused = false;
                messageBus.post(new AdEvent(RESUMED));
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
                messageBus.post(new AdEvent(AdEvent.Type.SKIPPED));
                cancelAdDisplayedCheckTimer();
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
            case  AD_BREAK_ENDED:
                messageBus.post(new AdEvent(AD_BREAK_ENDED));
                break;
            case  CUEPOINTS_CHANGED:
                sendCuePointsUpdate();
                break;
            case LOG:
                //for this case no AD ERROR is fired need to show view {type=adLoadError, errorCode=1009, errorMessage=The response does not contain any valid ads.}
                if (adEvent.getAd() == null) {
                    if (pkAdProviderListener != null) {
                        pkAdProviderListener.onAdLoadingFinished();
                    }
                    log.e("Ad is null - back to playback");
                    if (player != null && player.getView() != null) {
                        player.getView().showVideoSurface();
                    }
                }
                String error = "Unknown Error";
                if (adEvent.getAdData() != null) {
                    if (adEvent.getAdData().containsKey("errorMessage")) {
                        error = adEvent.getAdData().get("errorMessage");
                    }
                    log.e("Ad Error " + error);
                }
                if (!isAdDisplayed) {
                    cancelAdDisplayedCheckTimer();
                }

                messageBus.post(new AdError(AdError.Type.QUIET_LOG_ERROR, error));
                break;
            default:
                break;
        }
    }

    private void initAdDisplayedCheckTimer() {
        final Handler handler = new Handler();
        adDisplayedCheckTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if (adsManager != null && adsManager.getAdProgress() != null) {
                                float currentTime = adsManager.getAdProgress().getCurrentTime();
                                if (currentTime > 0) {
                                    log.d("AD Displayed delay check : ad duration " + currentTime);
                                    isAdDisplayed = true;
                                    messageBus.post(new AdEvent(AdEvent.Type.AD_DISPLAYED_AFTER_CONTENT_PAUSE));
                                    adDisplayedCheckTimer.cancel();
                                }
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        adDisplayedCheckTimer.schedule(timerTask, 0, IMAConfig.DEFAULT_AD_LOAD_COUNT_DOWN_TICK);
        messageBus.post(new AdEvent(AD_LOAD_TIMEOUT_TIMER_STARTED));
    }

    private void cancelAdDisplayedCheckTimer() {
        if (adDisplayedCheckTimer != null) {
            adDisplayedCheckTimer.cancel();
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
        String adDescription      = ad.getDescription();
        long adDuration           = (long)(ad.getDuration() * Consts.MILLISECONDS_MULTIPLIER);
        String adTitle            = ad.getTitle();
        boolean isAdSkippable     = ad.isSkippable();
        String contentType        = ad.getContentType();
        String adId               = ad.getAdId();
        String adSystem           = ad.getAdSystem();
        int adHeight              = ad.getHeight();
        int adWidth               = ad.getWidth();
        int adPodCount            = ad.getAdPodInfo().getTotalAds();
        int adPodPosition         = ad.getAdPodInfo().getAdPosition();
        long adPodTimeOffset      = (long)(ad.getAdPodInfo().getTimeOffset() * Consts.MILLISECONDS_MULTIPLIER);



        AdInfo adInfo =  new AdInfo(adDescription, adDuration,
                adTitle, isAdSkippable,
                contentType, adId,
                adSystem, adHeight,
                adWidth,
                adPodCount,
                adPodPosition,
                adPodTimeOffset);

        log.v("AdInfo: " + adInfo.toString());
        return adInfo;

    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {

        log.e("Ad Error: " + adErrorEvent.getError().getErrorCode().name() + " " + adErrorEvent.getError().getMessage());
        isAdRequested = true;
        isAdDisplayed = false;

        cancelAdDisplayedCheckTimer();
        String errorMessage = adErrorEvent.getError().getMessage();
        switch (adErrorEvent.getError().getErrorCode()) {
            case INTERNAL_ERROR:
                messageBus.post(new AdError(AdError.Type.INTERNAL_ERROR, errorMessage));
                break;
            case VAST_MALFORMED_RESPONSE:
                messageBus.post(new AdError(AdError.Type.VAST_MALFORMED_RESPONSE, errorMessage));
                break;
            case UNKNOWN_AD_RESPONSE:
                messageBus.post(new AdError(AdError.Type.UNKNOWN_AD_RESPONSE, errorMessage));
                break;
            case VAST_LOAD_TIMEOUT:
                messageBus.post(new AdError(AdError.Type.VAST_LOAD_TIMEOUT, errorMessage));
                break;
            case VAST_TOO_MANY_REDIRECTS:
                messageBus.post(new AdError(AdError.Type.VAST_TOO_MANY_REDIRECTS, errorMessage));
                break;
            case VIDEO_PLAY_ERROR:
                messageBus.post(new AdError(AdError.Type.VIDEO_PLAY_ERROR, errorMessage));
                break;
            case VAST_MEDIA_LOAD_TIMEOUT:
                messageBus.post(new AdError(AdError.Type.VAST_MEDIA_LOAD_TIMEOUT, errorMessage));
                break;
            case VAST_LINEAR_ASSET_MISMATCH:
                messageBus.post(new AdError(AdError.Type.VAST_LINEAR_ASSET_MISMATCH, errorMessage));
                break;
            case OVERLAY_AD_PLAYING_FAILED:
                messageBus.post(new AdError(AdError.Type.OVERLAY_AD_PLAYING_FAILED, errorMessage));
                break;
            case OVERLAY_AD_LOADING_FAILED:
                messageBus.post(new AdError(AdError.Type.OVERLAY_AD_LOADING_FAILED, errorMessage));
                break;
            case VAST_NONLINEAR_ASSET_MISMATCH:
                messageBus.post(new AdError(AdError.Type.VAST_NONLINEAR_ASSET_MISMATCH, errorMessage));
                break;
            case COMPANION_AD_LOADING_FAILED:
                messageBus.post(new AdError(AdError.Type.COMPANION_AD_LOADING_FAILED, errorMessage));
                break;
            case UNKNOWN_ERROR:
                messageBus.post(new AdError(AdError.Type.UNKNOWN_ERROR, errorMessage));
                break;
            case VAST_EMPTY_RESPONSE:
                messageBus.post(new AdError(AdError.Type.VAST_EMPTY_RESPONSE, errorMessage));
                break;
            case FAILED_TO_REQUEST_ADS:
                messageBus.post(new AdError(AdError.Type.FAILED_TO_REQUEST_ADS, errorMessage));
                break;
            case VAST_ASSET_NOT_FOUND:
                messageBus.post(new AdError(AdError.Type.VAST_ASSET_NOT_FOUND, errorMessage));
                break;
            case ADS_REQUEST_NETWORK_ERROR:
                messageBus.post(new AdError(AdError.Type.ADS_REQUEST_NETWORK_ERROR, errorMessage));
                break;
            case INVALID_ARGUMENTS:
                messageBus.post(new AdError(AdError.Type.INVALID_ARGUMENTS, errorMessage));
                break;
            case PLAYLIST_NO_CONTENT_TRACKING:
                messageBus.post(new AdError(AdError.Type.PLAYLIST_NO_CONTENT_TRACKING, errorMessage));
                break;
            default:
                messageBus.post(new AdError(AdError.Type.UNKNOWN_ERROR, errorMessage));
        }

        if (pkAdProviderListener != null) {
            pkAdProviderListener.onAdLoadingFinished();
        }

        if (player != null && player.getView() != null) {
            player.getView().showVideoSurface();
            player.play();
        }
    }
}
