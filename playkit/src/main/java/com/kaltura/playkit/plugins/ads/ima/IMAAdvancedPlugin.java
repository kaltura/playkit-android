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
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
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

public class IMAAdvancedPlugin extends PKPlugin implements AdsProvider , com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener {

    private static final PKLog log = PKLog.get("IMAAdvancedPlugin");



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
    private ImaSdkFactory sdkFactory;
    private AdsManager adsManager;
    private ImaSdkSettings imaSdkSettings;
    private AdsRenderingSettings renderingSettings;
    // Whether an ad is displayed.
    private boolean isAdDisplayed;
    private boolean isAdIsPaused;
    private boolean isAdRequested;
    private boolean isInitWaiting;
    private boolean isAllAdsCompleted;
    ////////////////////
    private MessageBus messageBus;


    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "IMAAdvanced";
        }

        @Override
        public PKPlugin newInstance() {
            return new IMAAdvancedPlugin();
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
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        this.isAllAdsCompleted = false;

        if (this.messageBus == null) {
            this.messageBus = messageBus;
            this.messageBus.listen(new PKEvent.Listener() {
                @Override
                public void onEvent(PKEvent event) {
                    log.d("Received:PlayerEvent:" + event.eventType().name());
                    AdCuePoints adCuePoints = new AdCuePoints(getAdCuePoints());
                    if (event.eventType() == PlayerEvent.Type.ENDED) {
                        if (isAllAdsCompleted || !adCuePoints.hasPostRoll() || adInfo == null || (adInfo.getAdIndexInPod() == adInfo.getTotalAdsInPod())) {
                            log.d("contentCompleted on ended");
                            contentCompleted();
                        } else {
                            log.d("contentCompleted delayed");
                            //isContentEndedBeforeMidroll = true;
                        }
                    }
                }
            }, PlayerEvent.Type.ENDED);
        }
        mVideoPlayerWithAdPlayback = new ExoPlayerWithAdPlayback(context);
        player.getView().addView(mVideoPlayerWithAdPlayback.getExoPlayerView());
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
        //isAutoPlay = false;
        isAdRequested = false;
        isAdDisplayed = false;
        isAllAdsCompleted = false;
        //isContentEndedBeforeMidroll = false;
        //lastEventReceived = null;
        if (adsManager != null) {
            adsManager.destroy();
        }
        clearAdsLoader();
        imaSetup();
        requestAdsFromIMA(adConfig.getAdTagURL());
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
    protected void onUpdateConfig(Object config) {

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
    public void start() {
        if (mAdsManager != null) {
            mAdsManager.start();
        }
//        isAutoPlay = true; // start will be called only on first time media is played programmatically
//        isAdRequested = true;
//        if (adsManager != null) {
//            log.d("IMA adsManager.init called");
//            if (appIsInBackground) {
//                log.d("Start: Ad Manager Init : " + adManagerInitDuringBackground);
//                adManagerInitDuringBackground = true;
//            } else {
//                adsManager.init(renderingSettings);
//            }
//        } else {
//            isInitWaiting = true;
//        }
    }

    @Override
    public void destroyAdsManager() {
        if (mAdsManager != null) {
            mAdsManager.destroy();
        }
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
        log.d("contentCompleted");
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
    public boolean isAllAdsCompleted() {
        return false;
    }

    @Override
    public boolean isAdError() {
        return false;
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
    public void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController) {

    }

    @Override
    public void removeAdProviderListener() {

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

        return adInfo;

    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {


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

                mAdsManager.addAdErrorListener(IMAAdvancedPlugin.this);
                mAdsManager.addAdEventListener(IMAAdvancedPlugin.this);

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