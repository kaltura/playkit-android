package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEnabledPlayerController;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;

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
    private String adTagURL;
    private List<String> mimeTypes;
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
        this.context = context;
        this.messageBus = messageBus;
        this.messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "onLoad:PlayerEvent:" + event);
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
        AdsConfig adConfig = AdsConfig.fromJsonObject(pluginConfig);
        adTagURL = adConfig.getAdTagUrl();//pluginConfig.getAsJsonPrimitive(AdsConfig.AD_TAG_URL).getAsString();
        mimeTypes = adConfig.getVideoMimeTypes();
        mAdUiContainer = (ViewGroup)player.getView();
        requestAd();
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
    public void requestAd() {
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);

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
        requestAds(adTagURL);
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
                if (mIsAdDisplayed || player == null || player.getDuration() <= 0) {
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
        return false;
    }

    @Override
    public void resume() {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.resume();
        } else {
            player.play();
        }
    }

    @Override
    public void pause() {
        if (mAdsManager != null && mIsAdDisplayed) {
            mAdsManager.pause();
        } else {
            player.pause();
        }
    }

    @Override
    public void contentCompleted() {
        player.play();
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
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
        switch (adErrorEvent.getError().getErrorCode()) {
            case INTERNAL_ERROR:
                messageBus.post(IMAEvents.IMA_INTERNAL_ERROR);
                break;
            case VAST_MALFORMED_RESPONSE:
                messageBus.post(IMAEvents.IMA_VAST_MALFORMED_RESPONSE);
                break;
            case UNKNOWN_AD_RESPONSE:
                messageBus.post(IMAEvents.IMA_UNKNOWN_AD_RESPONSE);
                break;
            case VAST_LOAD_TIMEOUT:
                messageBus.post(IMAEvents.IMA_VAST_LOAD_TIMEOUT);
                break;
            case VAST_TOO_MANY_REDIRECTS:
                messageBus.post(IMAEvents.IMA_VAST_TOO_MANY_REDIRECTS);
                break;
            case VIDEO_PLAY_ERROR:
                messageBus.post(IMAEvents.IMA_VIDEO_PLAY_ERROR);
                break;
            case VAST_MEDIA_LOAD_TIMEOUT:
                messageBus.post(IMAEvents.IMA_VAST_MEDIA_LOAD_TIMEOUT);
                break;
            case VAST_LINEAR_ASSET_MISMATCH:
                messageBus.post(IMAEvents.IMA_VAST_LINEAR_ASSET_MISMATCH);
                break;
            case OVERLAY_AD_PLAYING_FAILED:
                messageBus.post(IMAEvents.IMA_OVERLAY_AD_PLAYING_FAILED);
                break;
            case OVERLAY_AD_LOADING_FAILED:
                messageBus.post(IMAEvents.IMA_OVERLAY_AD_LOADING_FAILED);
                break;
            case VAST_NONLINEAR_ASSET_MISMATCH:
                messageBus.post(IMAEvents.IMA_VAST_NONLINEAR_ASSET_MISMATCH);
                break;
            case COMPANION_AD_LOADING_FAILED:
                messageBus.post(IMAEvents.IMA_COMPANION_AD_LOADING_FAILED);
                break;
            case UNKNOWN_ERROR:
                messageBus.post(IMAEvents.IMA_UNKNOWN_ERROR);
                break;
            case VAST_EMPTY_RESPONSE:
                messageBus.post(IMAEvents.IMA_VAST_EMPTY_RESPONSE);
                break;
            case FAILED_TO_REQUEST_ADS:
                messageBus.post(IMAEvents.IMA_FAILED_TO_REQUEST_ADS);
                break;
            case VAST_ASSET_NOT_FOUND:
                messageBus.post(IMAEvents.IMA_VAST_ASSET_NOT_FOUND);
                break;
            case ADS_REQUEST_NETWORK_ERROR:
                messageBus.post(IMAEvents.IMA_ADS_REQUEST_NETWORK_ERROR);
                break;
            case INVALID_ARGUMENTS:
                messageBus.post(IMAEvents.IMA_INVALID_ARGUMENTS);
                break;
            case PLAYLIST_NO_CONTENT_TRACKING:
                messageBus.post(IMAEvents.IMA_PLAYLIST_NO_CONTENT_TRACKING);
                break;
            default:
                messageBus.post(IMAEvents.IMA_UNKNOWN_ERROR);
        }
        player.play();
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
                messageBus.post(IMAEvents.IMA_STARTED);
                break;
            case PAUSED:
                mIsAdIsPaused = true;
                messageBus.post(IMAEvents.IMA_PAUSED);
                break;
            case RESUMED:
                mIsAdIsPaused = false;
                messageBus.post(IMAEvents.IMA_RESUMED);
                break;
            case COMPLETED:
                messageBus.post(IMAEvents.IMA_COMPLETED);
                break;
            case FIRST_QUARTILE:
                messageBus.post(IMAEvents.IMA_FIRST_QUARTILE);
                break;
            case MIDPOINT:
                messageBus.post(IMAEvents.IMA_MIDPOINT);
                break;
            case THIRD_QUARTILE:
                messageBus.post(IMAEvents.IMA_THIRD_QUARTILE);
                break;
            case SKIPPED:
                messageBus.post(IMAEvents.IMA_SKIPPED);
                break;
            case CLICKED:
                 mIsAdIsPaused = true;
                messageBus.post(IMAEvents.IMA_CLICKED);
                break;
            case TAPPED:
                messageBus.post(IMAEvents.IMA_TAPPED);
                break;
            case ICON_TAPPED:
                messageBus.post(IMAEvents.IMA_ICON_TAPPED);
                break;
            case AD_BREAK_READY:
                messageBus.post(IMAEvents.IMA_AD_BREAK_READY);
                break;
            case AD_PROGRESS:
                messageBus.post(IMAEvents.IMA_AD_PROGRESS);
                break;
            case AD_BREAK_STARTED:
                messageBus.post(IMAEvents.IMA_AD_BREAK_STARTED);
                break;
            case  AD_BREAK_ENDED:
                messageBus.post(IMAEvents.IMA_AD_BREAK_ENDED);
                break;
            case  CUEPOINTS_CHANGED:
                messageBus.post(IMAEvents.IMA_CUEPOINTS_CHANGED);
                break;
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                messageBus.post(IMAEvents.IMA_LOADED);
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                messageBus.post(IMAEvents.IMA_CONTENT_PAUSE_REQUESTED);
                mIsAdDisplayed = true;
                player.pause();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                messageBus.post(IMAEvents.IMA_CONTENT_PAUSE_REQUESTED);
                mIsAdDisplayed = false;
                player.play();
                break;
            case ALL_ADS_COMPLETED:
                messageBus.post(IMAEvents.IMA_ALL_ADS_COMPLETED);
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
