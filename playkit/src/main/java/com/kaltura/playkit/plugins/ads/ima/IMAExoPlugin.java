package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.view.View;
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
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdError;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsProvider;

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
    IMAConfig adConfig;
    //////////////////////


    /////////////////////

    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    // Ad-enabled video player.
    private ExoPlayerWithAdPlayback mVideoPlayerWithAdPlayback;

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
    // Whether an ad is displayed.
    private boolean isAdDisplayed;
    private boolean isAdIsPaused;
    private boolean isAdRequested = false;
    private boolean isInitWaiting = false;
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
    };

    ////////PKPlugin

    ///////////END PKPlugin
    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.player = player;
        if (player == null) {
            log.e("Error, player instance is null.");
            return;
        }
        mVideoPlayerWithAdPlayback = new ExoPlayerWithAdPlayback(context);
        //mVideoPlayerWithAdPlayback.setId(new Integer(123456789));

        player.getView().addView(mVideoPlayerWithAdPlayback.getSimpleExoPlayerView());
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
        requestAd();
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
            requestAd();
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
        pause();
    }

    @Override
    protected void onApplicationResumed() {

    }

    @Override
    protected void onDestroy() {
        log.d("IMA Start onDestroy");

        if (mAdsManager != null) {
            mAdsManager.destroy();
            mAdsManager = null;
        }
        if (adsLoader != null) {
            adsLoader.removeAdsLoadedListener(adsLoadedListener);
            adsLoadedListener = null;
            adsLoader = null;
        }

    }


    ////////Ads Plugin

    @Override
    public IMAConfig getAdsConfig() {
        return adConfig;
    }

    @Override
    public void requestAd() {
        log.d("Start RequestAd");
        if (imaSdkSettings == null) {
            imaSdkSettings = new ImaSdkSettings();
        }
        // Tell the SDK we want to control ad break playback.
        imaSdkSettings.setAutoPlayAdBreaks(adConfig.getAutoPlayAdBreaks());
        imaSdkSettings.setLanguage(adConfig.getLanguage());
        if (mSdkFactory == null) {
            mSdkFactory = ImaSdkFactory.getInstance();
        }
        if (adsLoader == null) {
            adsLoader = mSdkFactory.createAdsLoader(context, imaSdkSettings);
            // Add listeners for when ads are loaded and for errors.
            adsLoader.addAdsLoadedListener(getAdsLoadedListener());
        }
        if (adConfig != null) {
            requestAdsFromIMA(adConfig.getAdTagURL());
        }
    }

    @Override
    public void init() {
        isAdRequested = true;
        if(mAdsManager != null) {
            mAdsManager.init(renderingSettings);
            sendCuePointsUpdate();
        } else{
            isInitWaiting = true;
        }
    }

    private void requestAdsFromIMA(String adTagUrl) {
        log.d("Do requestAdsFromIMA");
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(mVideoPlayerWithAdPlayback.getVideoAdPlayer());
        mAdDisplayContainer.setAdContainer(mVideoPlayerWithAdPlayback.getAdUiContainer());

        // Set up spots for companions.

        ViewGroup adCompanionViewGroup = null;
        if (adCompanionViewGroup != null) {
            CompanionAdSlot companionAdSlot = mSdkFactory.createCompanionAdSlot();
            companionAdSlot.setContainer(adCompanionViewGroup);
            companionAdSlot.setSize(728, 90);
            ArrayList<CompanionAdSlot> companionAdSlots = new ArrayList<CompanionAdSlot>();
            companionAdSlots.add(companionAdSlot);
            mAdDisplayContainer.setCompanionSlots(companionAdSlots);
        }


        // Create the ads request.
        final AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(mVideoPlayerWithAdPlayback.getContentProgressProvider());

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

    @Override
    public boolean start() {
        //if (mAdsManager != null && isAdDisplayed) {
        //    mAdsManager.resume();
        //}
        mVideoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
        return true;
    }

    @Override
    public void resume() {
        log.d("AD Event resume mIsAdDisplayed = " + isAdDisplayed);
        if (isAdDisplayed) {
            mVideoPlayerWithAdPlayback.getVideoAdPlayer().playAd();
        }
    }

    @Override
    public void pause() {
        log.d("AD Event pause mIsAdDisplayed = " + isAdDisplayed);
        if (isAdDisplayed) {
            mVideoPlayerWithAdPlayback.getVideoAdPlayer().pauseAd();
        }
    }

    @Override
    public void contentCompleted() {
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
        return new Float(mVideoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getDuration()).longValue();
    }

    @Override
    public long getCurrentPosition() {
        return new Float(mVideoPlayerWithAdPlayback.getVideoAdPlayer().getAdProgress().getCurrentTime()).longValue();
    }

    @Override
    public void skipAd() {
        if (mAdsManager != null) {
            mAdsManager.skip();
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
        if (mAdsManager != null && mAdsManager.getAdCuePoints() != null) {
            for (Float cuePoint : mAdsManager.getAdCuePoints()) {
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
        String adDescription      = ad.getDescription();
        long adDuration           = (long)(ad.getDuration() * 1000);
        String adTitle            = ad.getTitle();
        boolean isAdSkippable     = ad.isSkippable();
        String contentType        = ad.getContentType();
        String adId               = ad.getAdId();
        String adSystem           = ad.getAdSystem();
        int adHeight              = ad.getHeight();
        int adWidth               = ad.getWidth();
        int adPodCount            = ad.getAdPodInfo().getTotalAds();
        int adPodPosition         = ad.getAdPodInfo().getAdPosition();
        long adPodTimeOffset      = (long)(ad.getAdPodInfo().getTimeOffset() * 1000);


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
        if (player != null && player.getView() != null) {
            player.getView().showVideoSurface();
            player.play();
        }
    }

    @Override
    public void onAdEvent(com.google.ads.interactivemedia.v3.api.AdEvent adEvent) {
        log.i("Event: " + adEvent.getType());
        if (adEvent.getAdData() != null) {
            log.i("Event: " + adEvent.getAdData().toString());
        }

        if (mAdsManager == null) {
            return;
        }
        switch (adEvent.getType()) {

            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                messageBus.post(new AdEvent(AdEvent.Type.LOADED));
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                log.d("AD_CONTENT_PAUSE_REQUESTED");
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
                    player.getView().removeView(player.getView().findViewById(new Integer(123456789)));
                    player.getView().showVideoSurface();
                    player.play();
                }
                break;
            case ALL_ADS_COMPLETED:
                log.d("AD_ALL_ADS_COMPLETED");
                messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                player.getView().showVideoSurface();
                if (mAdsManager != null) {
                    log.d("AD_ALL_ADS_COMPLETED onDestroy");
                    onDestroy();
                }

                break;
            case STARTED:
                log.d("AD STARTED");
                isAdIsPaused = false;
                adInfo = createAdInfo(adEvent.getAd());
                messageBus.post(new AdEvent.AdStartedEvent(adInfo));
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
                messageBus.post(new AdEvent(AdEvent.Type.SKIPPED));
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
                mAdsManager.start();
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


    private AdsLoader.AdsLoadedListener getAdsLoadedListener() {
        if (adsLoadedListener != null) {
            return adsLoadedListener;
        }
        adsLoadedListener = new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                mAdsManager = adsManagerLoadedEvent.getAdsManager();

                //Attach event and error event listeners.

                mAdsManager.addAdErrorListener(IMAExoPlugin.this);
                mAdsManager.addAdEventListener(IMAExoPlugin.this);

                renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
                if (adConfig.getVideoMimeTypes().size() > 0) {
                    renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
                }

                //if both are false we remove the support int ad count down in ad
                if (!adConfig.getAdAttribution() && !adConfig.getAdCountDown()) {
                    renderingSettings.setUiElements(Collections.<UiElement>emptySet());
                }

                if (isInitWaiting) {
                    mAdsManager.init(renderingSettings);
                    sendCuePointsUpdate();
                    isInitWaiting = false;
                }

            }
        };
        return adsLoadedListener;
    }

}
