package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEnabledPlayerController;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.plugins.ads.PKAdEvents;

import java.util.List;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMASimplePlugin extends PKPlugin implements AdsProvider, AdEvent.AdEventListener, AdErrorEvent.AdErrorListener  {

    private static final String TAG = "IMASimplePlugin";


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
            Log.e(TAG, "Error, player instance is null.");
            return;
        }
        this.context = context;
        this.messageBus = messageBus;
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "XXXXX onLoad:PlayerEvent:" + event);
            }
        }, PlayerEvent.PLAY);

        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "onLoad:PlayerEvent:" + event);

            }
        }, PlayerEvent.PAUSE);
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "onLoad:PlayerEvent:PlayerEvent.ENDED-" + event);
            }
        }, PlayerEvent.ENDED);

        //----------------------------//
        adConfig = AdsConfig.fromJsonObject(pluginConfig);
        //adTagURL = adConfig.getAdTagUrl();//pluginConfig.getAsJsonPrimitive(AdsConfig.AD_TAG_URL).getAsString();
        //mimeTypes = adConfig.getVideoMimeTypes();
        mAdUiContainer = (ViewGroup) player.getView();
        //requestAd();
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    @Override
    protected void onDestroy() {

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

                //AdsRenderingSettings renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
                //renderingSettings.setMimeTypes(mimeTypes);
                //renderingSettings.setUiElements(Collections.<UiElement>emptySet());
                mAdsManager.init();
            }
        });
        if (adConfig != null) {
            requestAds(adConfig.getAdTagUrl());
        }
    }

    private void requestAds(String adTagUrl) {
        AdDisplayContainer adDisplayContainer = mSdkFactory.createAdDisplayContainer();
        adDisplayContainer.setAdContainer(mAdUiContainer);

        // Create the ads request.
        AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(adDisplayContainer);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (mIsAdDisplayed || player == null || (player != null && player.getDuration() <= 0)) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }

                return new VideoProgressUpdate(player.getCurrentPosition(),
                            player.getDuration());
            }
        });

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        mAdsLoader.requestAds(request);
    }

    @Override
    public boolean start(boolean showLoadingView) {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.resume();
        } else {
            if (player != null) {
                player.play();
            }
        }
        return true;
    }

    @Override
    public void resume() {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.resume();
        } else {
            if (player != null) {
                player.play();
            }
        }
    }

    @Override
    public void pause() {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.pause();
        } else {
            if (player != null) {
                player.pause();
            }
        }
    }

    @Override
    public void contentCompleted() {
        if (player != null) {
            player.play();
        }
    }

    @Override
    public PKAdInfo getAdInfo() {
        return adInfo;
    }

    @Override
    public boolean isAdDisplayed() {
        Log.d(TAG, "IMASimplePlugin isAdDisplayed: " + mIsAdDisplayed);
        return mIsAdDisplayed;
    }

    @Override
    public boolean isAdPaused() {
        Log.d(TAG, "IMASimplePlugin isAdPaused: " + mIsAdIsPaused);
        return  mIsAdIsPaused;
    }

    @Override
    public boolean isAdRequested() {
        Log.d(TAG, "IMASimplePlugin isAdRequested: " + mIsAdRequested);
        return mIsAdRequested;
    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
        switch (adErrorEvent.getError().getErrorCode()) {
            case INTERNAL_ERROR:
                messageBus.post(PKAdEvents.AD_INTERNAL_ERROR);
                break;
            case VAST_MALFORMED_RESPONSE:
                messageBus.post(PKAdEvents.AD_VAST_MALFORMED_RESPONSE);
                break;
            case UNKNOWN_AD_RESPONSE:
                messageBus.post(PKAdEvents.AD_UNKNOWN_AD_RESPONSE);
                break;
            case VAST_LOAD_TIMEOUT:
                messageBus.post(PKAdEvents.AD_VAST_LOAD_TIMEOUT);
                break;
            case VAST_TOO_MANY_REDIRECTS:
                messageBus.post(PKAdEvents.AD_VAST_TOO_MANY_REDIRECTS);
                break;
            case VIDEO_PLAY_ERROR:
                messageBus.post(PKAdEvents.AD_VIDEO_PLAY_ERROR);
                break;
            case VAST_MEDIA_LOAD_TIMEOUT:
                messageBus.post(PKAdEvents.AD_VAST_MEDIA_LOAD_TIMEOUT);
                break;
            case VAST_LINEAR_ASSET_MISMATCH:
                messageBus.post(PKAdEvents.AD_VAST_LINEAR_ASSET_MISMATCH);
                break;
            case OVERLAY_AD_PLAYING_FAILED:
                messageBus.post(PKAdEvents.AD_OVERLAY_AD_PLAYING_FAILED);
                break;
            case OVERLAY_AD_LOADING_FAILED:
                messageBus.post(PKAdEvents.AD_OVERLAY_AD_LOADING_FAILED);
                break;
            case VAST_NONLINEAR_ASSET_MISMATCH:
                messageBus.post(PKAdEvents.AD_VAST_NONLINEAR_ASSET_MISMATCH);
                break;
            case COMPANION_AD_LOADING_FAILED:
                messageBus.post(PKAdEvents.AD_COMPANION_AD_LOADING_FAILED);
                break;
            case UNKNOWN_ERROR:
                messageBus.post(PKAdEvents.AD_UNKNOWN_ERROR);
                break;
            case VAST_EMPTY_RESPONSE:
                messageBus.post(PKAdEvents.AD_VAST_EMPTY_RESPONSE);
                break;
            case FAILED_TO_REQUEST_ADS:
                messageBus.post(PKAdEvents.AD_FAILED_TO_REQUEST_ADS);
                break;
            case VAST_ASSET_NOT_FOUND:
                messageBus.post(PKAdEvents.AD_VAST_ASSET_NOT_FOUND);
                break;
            case ADS_REQUEST_NETWORK_ERROR:
                messageBus.post(PKAdEvents.AD_ADS_REQUEST_NETWORK_ERROR);
                break;
            case INVALID_ARGUMENTS:
                messageBus.post(PKAdEvents.AD_INVALID_ARGUMENTS);
                break;
            case PLAYLIST_NO_CONTENT_TRACKING:
                messageBus.post(PKAdEvents.AD_PLAYLIST_NO_CONTENT_TRACKING);
                break;
            default:
                messageBus.post(PKAdEvents.AD_UNKNOWN_ERROR);
        }
        if (player != null) {
            player.play();
        }
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        Log.i(TAG, "Event: " + adEvent.getType());
        if (adEvent.getAdData() != null) {
            Log.i(TAG, "Event: " + adEvent.getAdData().toString());
        }
        switch (adEvent.getType()) {
            case STARTED:
                mIsAdIsPaused = false;
                messageBus.post(PKAdEvents.AD_STARTED);
                break;
            case PAUSED:
                mIsAdIsPaused = true;
                messageBus.post(PKAdEvents.AD_PAUSED);
                break;
            case RESUMED:
                mIsAdIsPaused = false;
                messageBus.post(PKAdEvents.AD_RESUMED);
                break;
            case COMPLETED:
                messageBus.post(PKAdEvents.AD_COMPLETED);
                break;
            case FIRST_QUARTILE:
                messageBus.post(PKAdEvents.AD_FIRST_QUARTILE);
                break;
            case MIDPOINT:
                messageBus.post(PKAdEvents.AD_MIDPOINT);
                break;
            case THIRD_QUARTILE:
                messageBus.post(PKAdEvents.AD_THIRD_QUARTILE);
                break;
            case SKIPPED:
                messageBus.post(PKAdEvents.AD_SKIPPED);
                break;
            case CLICKED:
                 mIsAdIsPaused = true;
                messageBus.post(PKAdEvents.AD_CLICKED);
                break;
            case TAPPED:
                messageBus.post(PKAdEvents.AD_TAPPED);
                break;
            case ICON_TAPPED:
                messageBus.post(PKAdEvents.AD_ICON_TAPPED);
                break;
            case AD_BREAK_READY:
                mAdsManager.start();
                messageBus.post(PKAdEvents.AD_AD_BREAK_READY);
                break;
            case AD_PROGRESS:
                messageBus.post(PKAdEvents.AD_AD_PROGRESS);
                break;
            case AD_BREAK_STARTED:
                messageBus.post(PKAdEvents.AD_AD_BREAK_STARTED);
                break;
            case  AD_BREAK_ENDED:
                messageBus.post(PKAdEvents.AD_AD_BREAK_ENDED);
                break;
            case  CUEPOINTS_CHANGED:
                messageBus.post(PKAdEvents.AD_CUEPOINTS_CHANGED);
                break;
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

                adInfo = new  AdInfo(adSescription,
                        adDuration,
                        adTitle,
                        isAdSkippable,
                        contentType,
                        adId,
                        adSystem,
                        adHeight,
                        adWidth,
                        traffickingParameters,
                        adPodInfo,
                        adCuePoints);


                messageBus.post(PKAdEvents.AD_LOADED);
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                messageBus.post(PKAdEvents.AD_CONTENT_PAUSE_REQUESTED);
                mIsAdDisplayed = true;
                if (player != null) {
                    player.pause();
                }
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                messageBus.post(PKAdEvents.AD_CONTENT_PAUSE_REQUESTED);
                mIsAdDisplayed = false;
                if (player != null) {
                    player.play();
                }
                break;
            case ALL_ADS_COMPLETED:
                messageBus.post(PKAdEvents.AD_ALL_ADS_COMPLETED);
                if (mAdsManager != null) {
                    mAdsManager.destroy();
                    mAdsManager = null;
                }
                break;
            default:
                break;
        }
    }



    ///////////END Ads Plugin
}
