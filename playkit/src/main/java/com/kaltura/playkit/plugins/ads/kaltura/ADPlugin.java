package com.kaltura.playkit.plugins.ads.kaltura;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.admanager.AdBreakEndedReason;
import com.kaltura.admanager.AdBreakInfo;
import com.kaltura.admanager.AdManager;
import com.kaltura.admanager.AdManagerAdErrorEvent;
import com.kaltura.admanager.AdManagerAdEvent;
import com.kaltura.admanager.AdPlayer;
import com.kaltura.admanager.AdUIController;
import com.kaltura.admanager.AdUiListener;
import com.kaltura.admanager.ContentProgressProvider;
import com.kaltura.admanager.DefaultAdManager;
import com.kaltura.admanager.DefaultAdUIController;
import com.kaltura.admanager.DefaultStringFetcher;
import com.kaltura.admanager.DefaultUrlPinger;
import com.kaltura.admanager.VideoProgressUpdate;
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
import com.kaltura.playkit.ads.PKAdBreakEndedReason;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.Set;

import static com.kaltura.admanager.AdManagerAdErrorEvent.Type.invalidArgumentsError;

public class ADPlugin extends PKPlugin implements AdsProvider {

    private static final PKLog log = PKLog.get("ADPlugin");

    private Player player;
    private Context context;
    private MessageBus messageBus;
    //private AdInfo adInfo;
    private ADConfig adConfig;
    private PKAdProviderListener pkAdProviderListener;
    private PKMediaConfig mediaConfig;
    private boolean isAllAdsCompleted;



    //---------------------------


    private AdManager.Listener adManagerListener;
    private DefaultUrlPinger urlPinger;
    private DefaultStringFetcher stringFetcher;
    private DefaultAdManager adManager;
    private long playingContentDuration = 0;

    AdInfo currentAdInfo;

    //------------------------------

    private boolean isAppInBackground = false;
    private boolean adManagerInitDuringBackground = false;
    private boolean isContentPrepared = false;
    private long adDuration;
    private boolean isAdDisplayed;
    private boolean isAdIsPaused;
    private boolean isAdRequested;
    private boolean isAdError;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "ADPlugin";
        }

        @Override
        public PKPlugin newInstance() {
            return new ADPlugin();
        }

        @Override
        public void warmUp(Context context) {
        }
    };

    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }


    @Override
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        this.isAllAdsCompleted = false;

        if (this.messageBus == null) {
            this.messageBus = messageBus;
            this.messageBus.listen(new PKEvent.Listener() {
                @Override
                public void onEvent(PKEvent event) {
                    if (!"progress".equals(event.eventType().name())) {
                        log.d("Received:PlayerEvent:" + event.eventType().name());
                    }
                }
            }, PlayerEvent.Type.ENDED);
        }
        adConfig = parseConfig(config);
        if (adConfig == null) {
            handleErrorEvent(new AdManagerAdErrorEvent(invalidArgumentsError, new Exception("AdConfig is null")));
            return;
        }
        setupAdManager();
    }

    private void setupAdManager() {
        adManagerListener = getAdManagerListener();
        urlPinger = new DefaultUrlPinger();
        stringFetcher = new DefaultStringFetcher();
        AdPlayer.Factory adPlayerFactory = new AdPlayer.Factory() {
            @Override
            public AdPlayer newPlayer() {
                AdPlayer adPlayer = new ADPlayer(context);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                View adPlayerView = adPlayer.getView();
                adPlayerView.setLayoutParams(layoutParams);
                return adPlayer;
            }
        };

        AdUIController.Factory adUIFactory =  new AdUIController.Factory() {
            @Override
            public AdUIController newAdController(AdUiListener listener, AdPlayer adPlayer) {
                return new DefaultAdUIController(context, listener, adConfig.getAdSkinContainer(), adConfig.getCompanionAdWidth(), adConfig.getCompanionAdHeight());
            }
        };

        ContentProgressProvider contentProgressProvider = new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (adManager == null || player == null) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                long currentPosition = player.getCurrentPosition();
                if (playingContentDuration <= 0) {
                    playingContentDuration = player.getDuration();
                }
                long duration = playingContentDuration;

                if (isAdDisplayed || currentPosition < 0 || duration <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(currentPosition, duration);
            }
        };

        AdManager.Settings settings = new AdManager.Settings();
        settings.preferredBitrate    = adConfig.getVideoBitrate();
        settings.preferredMimeType   = adConfig.getVideoMimeType();
        settings.adLoadTimeout       = adConfig.getAdLoadTimeOut();

        adManager = new DefaultAdManager(context, adPlayerFactory, contentProgressProvider, adUIFactory, stringFetcher, urlPinger, settings);
        adManager.addListener(getAdManagerListener());
    }

    private AdManager.Listener getAdManagerListener() {
        if (adManagerListener != null) {
            return adManagerListener;
        }

        adManagerListener = new AdManager.Listener() {

            @Override
            public void onAdEvent(AdManagerAdEvent adEvent) {
                if (adEvent.type != AdManagerAdEvent.Type.progress) {
                    log.d("received event " + adEvent.type + " isAppInBackground = " + isAppInBackground + " isContentPrepared = " + isContentPrepared);
                }
                switch(adEvent.type) {
                    case adTagRequested:
                        AdManagerAdEvent.AdRequestedEvent adRequestedEvent = (AdManagerAdEvent.AdRequestedEvent) adEvent;
                        log.d("received event onAdRequestedEvent adTagURL = " + adRequestedEvent.adTagURL);
                        messageBus.post(new AdEvent.AdRequestedEvent(adRequestedEvent.adTagURL));
                        break;

                    case adBreakPending:
                        adManager.createAdPlayer();
                        player.pause();
                        isAdRequested = true;
                        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_PENDING));
                        break;

                    case cuePointsChanged:
                        Set<Double> adTagCuePoints = ((AdManagerAdEvent.CuePointsChangedEvent) adEvent).cuePoints;
                        if (adTagCuePoints != null && !adTagCuePoints.contains(0.0)) {
                            isAdRequested = true;
                            preparePlayer(true);
                        }
                        messageBus.post(new AdEvent.AdCuePointsChangedEvent(adTagCuePoints));
                        break;

                    case adBreakStarted:
                        if (!isAppInBackground) {
                            ((ViewGroup) adConfig.getPlayerViewContainer()).removeAllViews();
                            ((ViewGroup) adConfig.getPlayerViewContainer()).addView(adManager.getAdPlayer().getView());
                            preparePlayer(false);
                        }
                        AdManagerAdEvent.AdBreakStartedEvent adBreakStartedEvent = (AdManagerAdEvent.AdBreakStartedEvent) adEvent;
                        currentAdInfo = createAdInfo(adBreakStartedEvent.adInfo, adBreakStartedEvent.adBreakInfo);
                        messageBus.post(new AdEvent.AdBreakStarted(currentAdInfo));
                        break;

                    case adBreakEnded:
                        AdManagerAdEvent.AdBreakEndedEvent adBreakEndedEvent = (AdManagerAdEvent.AdBreakEndedEvent) adEvent;
                        if (((AdManagerAdEvent.AdBreakEndedEvent) adEvent).removeAdPlayer){
                            removeAdPlayer(); // remove the player and the view
                        }
                        PKAdBreakEndedReason adBreakEndedReason = getPKAdBreakEndedReason(((AdManagerAdEvent.AdBreakEndedEvent) adEvent).adBreakEndedReason);
                        messageBus.post(new AdEvent.AdBreakEnded(adBreakEndedReason));

                        break;
                    case adsPlaybackEnded:
                        if (!player.isPlaying()) {
                            if (!isContentPrepared) {
                                isAdRequested = true; // in case we ignore adbreak
                                preparePlayer(true);
                            } else {
                                player.play();

                            }
                            messageBus.post(new AdEvent(AdEvent.Type.ADS_PLAYBACK_ENDED));
                        }
                        break;

                    case progress:
                        AdManagerAdEvent.AdProgressEvent adProgressEvent = (AdManagerAdEvent.AdProgressEvent) adEvent;
                        //log.d("received event " + adEvent.type + " " + adProgressEvent.currentPosition + "/" + adProgressEvent.duration);
                        messageBus.post(new AdEvent.AdProgressUpdateEvent(adProgressEvent.currentPosition, adProgressEvent.duration));
                        break;

                    case allAdsCompleted:
                        isAllAdsCompleted = true;
                        messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                        break;

                    case adClicked:
                        messageBus.post(new AdEvent.AdvtClickEvent(adEvent.adInfo.getClickThroughUrl()));
                        break;

                    //case adTouched:
                    //    break;
                    case adPaused:
                        isAdIsPaused = true;
                        messageBus.post(new AdEvent(AdEvent.Type.PAUSED));
                        break;

                    case adResumed:
                        isAdIsPaused = false;
                        if (!isAppInBackground) {
                            adConfig.getAdSkinContainer().setVisibility(View.VISIBLE);
                        }
                        messageBus.post(new AdEvent(AdEvent.Type.RESUMED));
                        break;

                    case adSkipped:
                        isAdDisplayed = false;
                        messageBus.post(new AdEvent(AdEvent.Type.SKIPPED));
                        break;

                    case adLoaded:
                        log.d("AD_LOADED");

                        if (!isAppInBackground) {
                            adManager.playAdBreak();
                        }
                        AdManagerAdEvent.AdLoadedEvent adLoadedEvent = (AdManagerAdEvent.AdLoadedEvent) adEvent;
                        if (adLoadedEvent.adInfo != null) {
                            adDuration = Double.valueOf(adEvent.adInfo.getDuration()).longValue();
                        }

                        currentAdInfo = createAdInfo(adEvent.adInfo, adLoadedEvent.adBreakInfo);
                        messageBus.post(new AdEvent.AdLoadedEvent(currentAdInfo));
                        break;

                    case adStarted:
                        log.d("AD_STARTED");

                        isAdDisplayed = true;
                        isAdError = false;
                        isAdIsPaused = false;
                        if (!isAppInBackground) {
                            adConfig.getAdSkinContainer().setVisibility(View.VISIBLE);
                        }
                        messageBus.post(new AdEvent(AdEvent.Type.STARTED));
                        break;

                    case adEnded:
                        isAdDisplayed = false;
                        adConfig.getAdSkinContainer().setVisibility(View.INVISIBLE);
                        messageBus.post(new AdEvent(AdEvent.Type.COMPLETED));
                        break;
                    case adBufferStart:
                        if (isAdDisplayed) {
                            messageBus.post(new AdEvent.AdBufferEvent(true));
                        }
                        break;

                    case adBufferEnd:
                        if (isAdDisplayed || (player.getCurrentPosition() > 0  && player.getCurrentPosition() >= player.getDuration())) {
                            messageBus.post(new AdEvent.AdBufferEvent(false));
                        }
                        break;

                    case firstQuartile:
                        messageBus.post(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
                        break;

                    case midpoint:
                        messageBus.post(new AdEvent(AdEvent.Type.MIDPOINT));
                        break;

                    case thirdQuartile:
                        messageBus.post(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onAdErrorEvent(AdManagerAdErrorEvent adErrorEvent) {
                log.e("received onAdErrorEvent " + adErrorEvent.type + " isAppInBackground = " + isAppInBackground + " isContentPrepared = " + isContentPrepared);
                handleErrorEvent(adErrorEvent);
            }
        };
        return adManagerListener;
    }

    private PKAdBreakEndedReason getPKAdBreakEndedReason(AdBreakEndedReason adBreakEndedReason) {
        switch (adBreakEndedReason) {
            case discarded:
                return PKAdBreakEndedReason.DISCARDED;
            case completed:
                return PKAdBreakEndedReason.COMPLETED;
            default:
                return PKAdBreakEndedReason.UNKNOWN;
        }
    }

    private AdInfo createAdInfo(com.kaltura.admanager.AdInfo adInfo, AdBreakInfo adBreakInfo) {
        String adDescription = adInfo.getDescription();
        long adDuration = (long) (adInfo.getDuration() * Consts.MILLISECONDS_MULTIPLIER);
        String adTitle = adInfo.getTitle();
        boolean isAdSkippable = adInfo.isSkippable();

        String adId = adInfo.getId();
        String adSystem = adInfo.getAdSystem();
        int adHeight = 0;
        int adWidth = 0;

        if (adInfo.getComapnionAdIdByResMap().containsKey(0)) {
            if (adInfo.getComapnionAdIdByResMap().get(0).size() > 0) {
                String companionAdResolution = adConfig.getCompanionAdWidth() + "_" + adConfig.getCompanionAdHeight();
                if (adInfo.getComapnionAdIdByResMap().get(0).containsKey(companionAdResolution)) {
                    adHeight = adConfig.getCompanionAdHeight();
                    adWidth =  adConfig.getCompanionAdWidth();
                    log.d("Found Key : " + companionAdResolution);
                }
            }
        }

        String contentType = adInfo.getAdMimeType();
        int totalAdsInPod = adInfo.getTotalAds();
        int adIndexInPod = adInfo.getAdIndex();   // index starts in 1
        int podCount = adBreakInfo.getTotalAdBreaks();
        int podIndex = adBreakInfo.getAdBreakIndex();
        boolean isBumper = adInfo.getTitle().toLowerCase().contains("bumper");
        long adPodTimeOffset = (long) (adInfo.getAdCuePoint() * Consts.MILLISECONDS_MULTIPLIER);


        AdInfo newAdInfo = new AdInfo(adDescription, adDuration,
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
        return newAdInfo;

    }

    private void handleErrorEvent(AdManagerAdErrorEvent adErrorEvent) {
        sendError(adErrorEvent);
        isAdRequested = true;
        isAdError = true;
        removeAdPlayer();
        if (isContentPrepared) {
            player.play();
        } else {
            preparePlayer(true);
        }
        //TODO Check if fatal error and clear all the adBreaks
    }

    //-----------------------------


    private void sendError(AdManagerAdErrorEvent adErrorEvent) {
        log.e("Ad Error: " + adErrorEvent.type + " with message " + adErrorEvent.exception.getMessage());

        String message = "";
        Throwable cause = null;
        if (adErrorEvent.exception != null) {
            if (adErrorEvent.exception.getMessage() != null) {
                message = adErrorEvent.exception.getMessage();
            }
            if (adErrorEvent.exception.getCause() != null) {
                cause = adErrorEvent.exception.getCause();
            }
        }
        PKAdErrorType pkErrorType = getPKErrorType(adErrorEvent.type);
        AdEvent.Error errorEvent = new AdEvent.Error(new PKError(pkErrorType, message, cause));
        messageBus.post(errorEvent);
    }

    private PKAdErrorType getPKErrorType(AdManagerAdErrorEvent.Type type) {
        switch (type) {
            case adLoadError:
                return PKAdErrorType.AD_LOAD_ERROR;
            case networkError:
                return PKAdErrorType.ADS_REQUEST_NETWORK_ERROR;
            case companionAdLoadingError:
                return PKAdErrorType.COMPANION_AD_LOADING_FAILED;
            case vmapEmptyResponseError:
                return PKAdErrorType.VMAP_EMPTY_RESPONSE;
            case vastEmptyResponseError:
                return PKAdErrorType.VAST_EMPTY_RESPONSE;
            case vastTooManyRedirects:
                return PKAdErrorType.VAST_TOO_MANY_REDIRECTS;
            case invalidArgumentsError:
                return PKAdErrorType.INVALID_ARGUMENTS;
            case noTrackingEventsError:
                return PKAdErrorType.NO_TRACKING_EVENTS;
            case videoPlayError:
                return PKAdErrorType.VIDEO_PLAY_ERROR;
            default:
                return PKAdErrorType.UNKNOWN_ERROR;
        }
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("Start onUpdateMedia");
        this.mediaConfig = mediaConfig;
        isAdRequested = false;
        isAdDisplayed = false;
        isAllAdsCompleted = false;
        isContentPrepared = false;
        playingContentDuration = 0;
        //isContentEndedBeforeMidroll = false;
        if (!adManagerInitDuringBackground) {
            if (adConfig != null) {
                AdManager.Settings settings = getSettings(mediaConfig);
                adManager.updateSettings(settings);

                requestForAds(adConfig.getAdTagURL());
            } else {
                preparePlayer(true);
            }
        }
    }

    @NonNull
    private AdManager.Settings getSettings(PKMediaConfig mediaConfig) {
        AdManager.Settings settings = new AdManager.Settings();
        settings.preferredBitrate    = adConfig.getVideoBitrate();
        settings.preferredMimeType   = adConfig.getVideoMimeType();
        settings.adLoadTimeout       = adConfig.getAdLoadTimeOut();
        settings.startAdFromPosition = mediaConfig.getStartPosition();
        return settings;
    }

    private void requestForAds(String adTagURL) {
        adManager.load(adTagURL);
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("Start onUpdateConfig");

        adConfig = parseConfig(config);
        isAdRequested = false;
        isAdDisplayed = false;
        isAdIsPaused  = false;
        isAdError     = false;
        isContentPrepared = false;
        isAllAdsCompleted = false;
    }

    @Override
    protected void onApplicationPaused() {
        isAppInBackground = true;
        adManager.onPause();
    }

    @Override
    protected void onApplicationResumed() {
        isAppInBackground = false;
        if (adManagerInitDuringBackground) {
            adManagerInitDuringBackground = false;
            setupAdManager();
            requestForAds(adConfig.getAdTagURL());
            return;
        }

        adManager.onResume();

    }

    @Override
    protected void onDestroy() {
        log.d("Start onDestroy");
        resetAdPlugin();
        if (adManager != null) {
            //adManager.destroy();
            adManager.removeAdPlayer();
            adManager.addListener(null);
            adManager = null;
        }

        isContentPrepared = false;
        removeAdProviderListener();
    }

    private void resetAdPlugin() {
        log.d("Start resetAdPlugin");
        isAdError = false;
        isAdRequested = false;
        isAdDisplayed = false;
        playingContentDuration = 0;
        if (adManager != null) {
            adManager.removeAdPlayer();
            adManager.addListener(null);
            //adManager.destroy();
            adManager = null;
        }
    }


// ------------------------------------------

    @Override
    public ADConfig getAdsConfig() {
        return adConfig;
    }

    @Override
    public void start() {
        if (adManager != null) {
            log.d("AD Plugin start called");
            if (isAppInBackground) {
                adManagerInitDuringBackground = true;
                log.d("Start: Ad Manager Init adManagerInitDuringBackground : " + adManagerInitDuringBackground);
            }
        } else {
            //setupAdManager();
        }
    }

    @Override
    public void destroyAdsManager() {
        resetAdPlugin();
    }

    @Override
    public void resume() {
        adManager.getAdPlayer().play();
    }

    @Override
    public void pause() {
        adManager.getAdPlayer().pause();
    }

    @Override
    public void contentCompleted() {

    }

    @Override
    public PKAdInfo getAdInfo() {
        return null;
    }

    @Override
    public boolean isAdDisplayed() {
        return isAdDisplayed;
    }

    @Override
    public boolean isAdPaused() {
        return isAdIsPaused;
    }

    @Override
    public boolean isAdRequested() {
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
        if (adManager != null && isAdDisplayed()) {
            if (adDuration == 0) {
                adDuration = (long) adManager.getAdPlayer().getPosition() / Consts.MILLISECONDS_MULTIPLIER;
                return adDuration;
            } else {
                return adDuration;
            }
        }
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        if (adManager != null && adManager.getAdPlayer() != null && isAdDisplayed()) {
            return (long) adManager.getAdPlayer().getPosition() / Consts.MILLISECONDS_MULTIPLIER;
        } else {
            return 0;
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
        adManager.onSkip();
    }

    @Override
    public void openLearnMore() {
        adManager.onAdClicked();
    }

    @Override
    public void openComapanionAdLearnMore() {
        adManager.onCompanionAdClicked();
    }

    @Override
    public void screenOrientationChanged(boolean isFullScreen) {
        if (isAdDisplayed()) {
            if (isFullScreen) {
                adManager.onExpand();
            } else {
                adManager.onCollapse();
            }
        }
    }

    @Override
    public void volumeKeySilent(boolean isMute) {
        if (isAdDisplayed()) {
            if (isMute) {
                adManager.onMute();
            } else {
                adManager.onUnMute();
            }
        }
    }

    private static ADConfig parseConfig(Object config) {
        if (config instanceof ADConfig) {
            return ((ADConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), ADConfig.class);
        }
        return null;
    }

    private void preparePlayer(boolean doPlay) {
        log.d("AdPlugin preparePlayer");
        if (pkAdProviderListener != null && !isAppInBackground) {
            log.d("AdPlugin doing prepare player");
            isContentPrepared = true;
            pkAdProviderListener.onAdLoadingFinished();
            if (doPlay) {
                messageBus.listen(new PKEvent.Listener() {
                    @Override
                    public void onEvent(PKEvent event) {
                        if (player != null && player.getView() != null) {
                            player.play();
                        }

                        messageBus.remove(this);
                    }
                }, PlayerEvent.Type.DURATION_CHANGE);
            }
        }
    }

    private void removeAdPlayer() {
        log.d("removeAdPlayer");
        if (adConfig != null) {
            adConfig.getAdSkinContainer().setVisibility(View.INVISIBLE);
        }
        adManager.removeAdPlayer();
        ((ViewGroup) adConfig.getPlayerViewContainer()).removeAllViews();
        ((ViewGroup) adConfig.getPlayerViewContainer()).addView(player.getView());
    }
}
