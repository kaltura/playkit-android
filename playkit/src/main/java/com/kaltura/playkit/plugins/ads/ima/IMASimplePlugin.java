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
import com.kaltura.playkit.plugins.ads.AdEnabledPlayerController;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.AdsPlugin;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMASimplePlugin extends PKPlugin implements AdsPlugin, AdEvent.AdEventListener, AdErrorEvent.AdErrorListener  {

    private static final String TAG = "IMASimplePlugin";


    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new AdEnabledPlayerController(this);
    }

    /////////////////////
    private Player player;
    private Context context;
    private String adTagURL;

    //////////////////////
    private PlayerConfig.Media mediaConfig;

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
    ////////////////////

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
        adTagURL = pluginConfig.getAsJsonPrimitive(AdsConfig.AD_TAG_URL).getAsString();
        mAdUiContainer = (ViewGroup)player.getView();
        requestAd();
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);
            }
        });
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
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
        player.play();
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        Log.i(TAG, "Event: " + adEvent.getType());

        // These are the suggested event types to handle. For full list of all ad event
        // types, see the documentation for AdEvent.AdEventType.
        switch (adEvent.getType()) {
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                mIsAdDisplayed = true;
                player.pause();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                mIsAdDisplayed = false;
                player.play();
                break;
            case ALL_ADS_COMPLETED:
                if (mAdsManager != null) {
                    mAdsManager.destroy();
                    mAdsManager = null;
                }
                break;
            default:
                break;
        }
    }

    public boolean ismIsAdDisplayed() {
        return mIsAdDisplayed;
    }

    ///////////END Ads Plugin
}
