package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
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
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEnabledPlayerController;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMASimplePlugin extends PKPlugin implements AdsProvider, com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener, AdErrorEvent.AdErrorListener  {

    private static final String TAG = "IMASimplePlugin";
    private static final PKLog log = PKLog.get(TAG);



    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }

    /////////////////////
    private Player player;
    private Context context;
    private AdInfo adInfo;
    AdsConfig adConfig;
    //////////////////////


    /////////////////////

    // The container for the ad's UI.
    private ViewGroup mAdUiContainer;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Whether an ad is displayed.
    private boolean mIsAdDisplayed;
    private boolean mIsAdIsPaused;
    private boolean mIsAdRequested = false;
    ////////////////////
    private MessageBus messageBus;


    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "IMASimplePlugin";
        }

        @Override
        public PKPlugin newInstance() {
            return new IMASimplePlugin();
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
        this.context = context;
        this.messageBus = messageBus;
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("onLoad:PlayerEvent:" + event);
            }
        }, PlayerEvent.Type.PLAY);

        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("onLoad:PlayerEvent:" + event);

            }
        }, PlayerEvent.Type.PAUSE);
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("onLoad:PlayerEvent:PlayerEvent.ENDED-" + event);
                contentCompleted();
            }
        }, PlayerEvent.Type.ENDED);

        //----------------------------//
        adConfig = AdsConfig.fromJsonObject(pluginConfig);
        mAdUiContainer = (ViewGroup) player.getView();
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        mIsAdRequested = false;
        mIsAdDisplayed = false;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        mIsAdRequested = false;
        mIsAdDisplayed = false;
    }

    @Override
    protected void onDestroy() {
        if (mAdsManager != null) {
            mAdsManager.destroy();
        }
        if (mAdsLoader != null) {
            mAdsLoader.contentComplete();
        }
    }


    ////////Ads Plugin

    @Override
    public String getPluginName() {
        return IMASimplePlugin.factory.getName();
    }

    @Override
    public AdsConfig getAdsConfig() {
        return adConfig;
    }

    @Override
    public void requestAd() {
        log.d("Start RequestAd");

        mIsAdRequested = true;
        ImaSdkSettings imaSdkSettings = new ImaSdkSettings();
        // Tell the SDK we want to control ad break playback.
        imaSdkSettings.setAutoPlayAdBreaks(adConfig.getAutoPlayAdBreaks());

        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context, imaSdkSettings);

        // Add listeners for when ads are loaded and for errors.
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                mAdsManager = adsManagerLoadedEvent.getAdsManager();

                //Attach event and error event listeners.

                mAdsManager.addAdErrorListener(IMASimplePlugin.this);
                mAdsManager.addAdEventListener(IMASimplePlugin.this);

                AdsRenderingSettings renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
                if (adConfig.getVideoMimeTypes().size() > 0) {
                    renderingSettings.setMimeTypes(adConfig.getVideoMimeTypes());
                }
                if (adConfig.getAdAttribution() || adConfig.getAdCountDown()) {
                    Set<UiElement> set = new HashSet<UiElement>();
                    if (adConfig.getAdAttribution()) {
                        set.add(UiElement.AD_ATTRIBUTION);
                    }
                    if (adConfig.getAdCountDown()) {
                        set.add(UiElement.COUNTDOWN);
                    }
                    renderingSettings.setUiElements(set);
                } else {
                    renderingSettings.setUiElements(Collections.<UiElement>emptySet());
                }
                mAdsManager.init();
            }
        });
        if (adConfig != null) {
            requestAdsFromIMA(adConfig.getAdTagUrl());
        }
    }

    private void requestAdsFromIMA(String adTagUrl) {
        log.d("Do requestAdsFromIMA");
        AdDisplayContainer adDisplayContainer = mSdkFactory.createAdDisplayContainer();
        adDisplayContainer.setAdContainer(mAdUiContainer);

        // Set up spots for companions.

        ViewGroup adCompanionViewGroup = null;
        if (adCompanionViewGroup != null) {
            CompanionAdSlot companionAdSlot = mSdkFactory.createCompanionAdSlot();
            companionAdSlot.setContainer(adCompanionViewGroup);
            companionAdSlot.setSize(728, 90);
            ArrayList<CompanionAdSlot> companionAdSlots = new ArrayList<CompanionAdSlot>();
            companionAdSlots.add(companionAdSlot);
            adDisplayContainer.setCompanionSlots(companionAdSlots);
        }

        // Create the ads request.
        final AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(adDisplayContainer);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {

                if (mIsAdDisplayed || player == null || (player != null && player.getDuration() <= 0)) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                VideoProgressUpdate videoProgress = new VideoProgressUpdate(player.getCurrentPosition(),
                        player.getDuration());
                //log.e("THE CONTENT PLAYER PROGRESS: getContentProgress " + videoProgress.getCurrentTime() + "/" + videoProgress.getDuration());
                return videoProgress;
            }
        });

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        mAdsLoader.requestAds(request);
    }

    @Override
    public boolean start(boolean showLoadingView) {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.resume();
//        } else {
//            if (player != null) {
//                player.play();
//            }
        }
        return true;
    }

    @Override
    public void resume() {
        log.e("AD Event pause mIsAdDisplayed = " + mIsAdDisplayed);
        if (mAdsManager != null) {
            if (mIsAdDisplayed) {
                mAdsManager.resume();
//            } else {
//                if (player != null) {
//                    player.play();
//                }
            }
        }
    }

    @Override
    public void pause() {
        log.e("AD Event pause mIsAdDisplayed = " + mIsAdDisplayed);
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.pause();
//        } else {
//            if (player != null) {
//                player.pause();
//            }
        }
    }

    @Override
    public void contentCompleted() {
        if (mAdsManager != null) {
            mAdsLoader.contentComplete();
        }
    }

    @Override
    public PKAdInfo getAdInfo() {
        return adInfo;
    }

    @Override
    public boolean isAdDisplayed() {
        log.d("IMASimplePlugin isAdDisplayed: " + mIsAdDisplayed);
        return mIsAdDisplayed;
    }

    @Override
    public boolean isAdPaused() {
        log.d("IMASimplePlugin isAdPaused: " + mIsAdIsPaused);
        return  mIsAdIsPaused;
    }

    @Override
    public boolean isAdRequested() {
        log.d("IMASimplePlugin isAdRequested: " + mIsAdRequested);
        return mIsAdRequested;
    }

    @Override
    public long getDuration() {
        return  (long)mAdsManager.getAdProgress().getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return (long)mAdsManager.getAdProgress().getCurrentTime();
    }



    @Override
    public void onAdEvent(com.google.ads.interactivemedia.v3.api.AdEvent adEvent) {
        log.i("Event: " + adEvent.getType());
        if (adEvent.getAdData() != null) {
            log.i("Event: " + adEvent.getAdData().toString());
        }
        switch (adEvent.getType()) {

            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.


                String adSescription      = adEvent.getAd().getDescription();
                double adDuration         = adEvent.getAd().getDuration();
                String adTitle            = adEvent.getAd().getTitle();
                boolean isAdSkippable     = adEvent.getAd().isSkippable();
                String contentType        = adEvent.getAd().getContentType();
                String adId               = adEvent.getAd().getAdId();
                String adSystem           = adEvent.getAd().getAdSystem();
                int adHeight              = adEvent.getAd().getHeight();
                int adWidth               = adEvent.getAd().getWidth();
                String traffickingParameters = adEvent.getAd().getTraffickingParameters();
                AdPodInfo adPodInfo          =  adEvent.getAd().getAdPodInfo();
                List<Float> adCuePoints      =  mAdsManager.getAdCuePoints();

                adInfo = new  AdInfo(adSescription, adDuration,
                        adTitle, isAdSkippable,
                        contentType, adId,
                        adSystem, adHeight,
                        adWidth, traffickingParameters,
                        adPodInfo, adCuePoints);

                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_LOADED));
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                log.d("AD_CONTENT_PAUSE_REQUESTED");
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_CONTENT_PAUSE_REQUESTED));
                mIsAdDisplayed = true;
                if (player != null) {
                    player.pause();
                }
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                log.d("AD REQUEST AD_CONTENT_RESUME_REQUESTED");
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_CONTENT_RESUME_REQUESTED));
                mIsAdDisplayed = false;
                if (player != null) {
                    player.play();
                }
                break;
            case ALL_ADS_COMPLETED:
                log.d("AD_ALL_ADS_COMPLETED");
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_ALL_ADS_COMPLETED));
                if (mAdsManager != null) {
                    //mAdsManager.destroy();
                    //mAdsManager = null;
                }
                break;
            case STARTED:
                log.d("AD STARTED");
                mIsAdIsPaused = false;
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_STARTED));
                break;
            case PAUSED:
                log.d("AD PAUSED");
                mIsAdIsPaused = true;
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_PAUSED));
                break;
            case RESUMED:
                log.d("AD RESUMED");
                mIsAdIsPaused = false;
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_RESUMED));
                break;
            case COMPLETED:
                log.d("AD COMPLETED");
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_COMPLETED));
                break;
            case FIRST_QUARTILE:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_FIRST_QUARTILE));
                break;
            case MIDPOINT:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_MIDPOINT));
                break;
            case THIRD_QUARTILE:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_THIRD_QUARTILE));
                break;
            case SKIPPED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_SKIPPED));
                break;
            case CLICKED:
                mIsAdIsPaused = true;
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_CLICKED));
                break;
            case TAPPED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_TAPPED));
                break;
            case ICON_TAPPED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_ICON_TAPPED));
                break;
            case AD_BREAK_READY:
                mAdsManager.start();
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_AD_BREAK_READY));
                break;
            case AD_PROGRESS:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_AD_PROGRESS));
                break;
            case AD_BREAK_STARTED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_AD_BREAK_STARTED));
                break;
            case  AD_BREAK_ENDED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_AD_BREAK_ENDED));
                break;
            case  CUEPOINTS_CHANGED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_CUEPOINTS_CHANGED));
                break;
            default:
                break;
        }
    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        log.e("Ad Error: " + adErrorEvent.getError().getMessage());
        switch (adErrorEvent.getError().getErrorCode()) {
            case INTERNAL_ERROR:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_INTERNAL_ERROR));
                break;
            case VAST_MALFORMED_RESPONSE:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_MALFORMED_RESPONSE));
                break;
            case UNKNOWN_AD_RESPONSE:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_UNKNOWN_AD_RESPONSE));
                break;
            case VAST_LOAD_TIMEOUT:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_LOAD_TIMEOUT));
                break;
            case VAST_TOO_MANY_REDIRECTS:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_TOO_MANY_REDIRECTS));
                break;
            case VIDEO_PLAY_ERROR:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VIDEO_PLAY_ERROR));
                break;
            case VAST_MEDIA_LOAD_TIMEOUT:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_MEDIA_LOAD_TIMEOUT));
                break;
            case VAST_LINEAR_ASSET_MISMATCH:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_LINEAR_ASSET_MISMATCH));
                break;
            case OVERLAY_AD_PLAYING_FAILED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_OVERLAY_AD_PLAYING_FAILED));
                break;
            case OVERLAY_AD_LOADING_FAILED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_OVERLAY_AD_LOADING_FAILED));
                break;
            case VAST_NONLINEAR_ASSET_MISMATCH:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_NONLINEAR_ASSET_MISMATCH));
                break;
            case COMPANION_AD_LOADING_FAILED:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_COMPANION_AD_LOADING_FAILED));
                break;
            case UNKNOWN_ERROR:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_UNKNOWN_ERROR));
                break;
            case VAST_EMPTY_RESPONSE:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_EMPTY_RESPONSE));
                break;
            case FAILED_TO_REQUEST_ADS:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_FAILED_TO_REQUEST_ADS));
                break;
            case VAST_ASSET_NOT_FOUND:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_VAST_ASSET_NOT_FOUND));
                break;
            case ADS_REQUEST_NETWORK_ERROR:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_ADS_REQUEST_NETWORK_ERROR));
                break;
            case INVALID_ARGUMENTS:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_INVALID_ARGUMENTS));
                break;
            case PLAYLIST_NO_CONTENT_TRACKING:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_PLAYLIST_NO_CONTENT_TRACKING));
                break;
            default:
                messageBus.post(new AdEvent.Generic(AdEvent.Type.AD_UNKNOWN_ERROR));
        }
        if (player != null) {
            player.play();
        }
    }



    ///////////END Ads Plugin
}
