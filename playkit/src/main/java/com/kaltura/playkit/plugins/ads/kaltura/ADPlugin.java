package com.kaltura.playkit.plugins.ads.kaltura;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.admanager.AdBreakEndedReason;
import com.kaltura.admanager.AdEvent;
import com.kaltura.admanager.AdInfo;
import com.kaltura.admanager.AdManager;
import com.kaltura.admanager.AdPlayer;
import com.kaltura.admanager.AdUIController;
import com.kaltura.admanager.AdUiListener;
import com.kaltura.admanager.DefaultAdManager;
import com.kaltura.admanager.DefaultAdUIController;
import com.kaltura.admanager.DefaultStringFetcher;
import com.kaltura.admanager.DefaultUrlPinger;
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
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.plugins.ads.AdsProvider;

import java.util.List;

public class ADPlugin extends PKPlugin implements AdsProvider {

    private static final PKLog log = PKLog.get("ADPlugin");

    private Player player;
    private Context context;
    private MessageBus messageBus;
    private AdInfo adInfo;
    private ADConfig adConfig;
    private PKAdProviderListener pkAdProviderListener;
    private PKMediaConfig mediaConfig;
    private boolean isAllAdsCompleted = false;
    private List<Double> adBreaksCuePoints;
    private DefaultAdUIController adUIController;
    private AdUiListener adUiListener;

    //---------------------------


    private AdManager.Listener adManagerListener;
    private DefaultUrlPinger urlPinger;
    private DefaultStringFetcher stringFetcher;
    private DefaultAdManager adManager;
    private AdPlayer adPlayer;

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
                    log.d("Received:PlayerEvent:" + event.eventType().name());
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
                }
            }, PlayerEvent.Type.ENDED);
        }
        adConfig = parseConfig(config);
        setupAdManager();
    }

    private void setupAdManager() {
        adManagerListener = getAdManagerListener();
        urlPinger = new DefaultUrlPinger();
        stringFetcher = new DefaultStringFetcher();
        AdPlayer.Factory adPlayerFactory = new AdPlayer.Factory() {
            @Override
            public AdPlayer newPlayer() {
                adPlayer = new ADPlayer(context);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                View adPlayerView = adPlayer.getView();
                adPlayerView.setLayoutParams(layoutParams);
                ((ViewGroup) adConfig.getPlayerViewContainer()).removeAllViews();
                ((ViewGroup) adConfig.getPlayerViewContainer()).addView(adPlayerView);
                return adPlayer;
            }
        };

        adManager = new DefaultAdManager(context, adPlayerFactory, null, new AdUIController.Factory() {
            @Override
            public AdUIController newAdController(AdUiListener listener, AdPlayer adPlayer) {
                adUIController = new DefaultAdUIController(context, getAdUiListener(), adPlayer);
                return adUIController;
            }
        }, stringFetcher, urlPinger, null/*Settings*/);
        adManager.addListener(getAdManagerListener());
    }


    public AdManager.Listener getAdManagerListener() {
        if (adManagerListener != null) {
            return adManagerListener;
        }

        adManagerListener = new AdManager.Listener() {

            @Override
            public void onAdEvent(AdEvent.AdEventType adEventType, AdInfo adInfo) {
                log.d("recieved event " + adEventType);
                switch(adEventType) {
                    case adBreakPending:
                        adManager.playAdBreak();
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onAdBreakEndedEvent(AdBreakEndedReason adBreakEndedReason) {
                log.d("onAdBreakEnded");
            }

            @Override
            public void onAdProgressEvent(AdInfo adInfo, long currentPosition, long adPlaybackPosition) {

            }

            @Override
            public void onAdErrorEvent(AdEvent.AdErrorEventType adErrorEventType, Exception ex) {

            }
        };
        return adManagerListener;
    }

    //-----------------------------



    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("Start onUpdateMedia");
        this.mediaConfig = mediaConfig;
        //isAdRequested = false;
        //isAdDisplayed = false;
        isAllAdsCompleted = false;
        //isContentEndedBeforeMidroll = false;

        requestForAds(adConfig.getAdTagURL());
    }

    private void requestForAds(String adTagURL) {
        adManager.load(adTagURL);
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("Start onUpdateConfig");

        adConfig = parseConfig(config);
        //isAdRequested = false;
        //isAdDisplayed = false;
        isAllAdsCompleted = false;
    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    @Override
    protected void onDestroy() {

    }


// ------------------------------------------

    @Override
    public ADConfig getAdsConfig() {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void destroyAdsManager() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

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
        return false;
    }

    @Override
    public boolean isAdPaused() {
        return false;
    }

    @Override
    public boolean isAdRequested() {
        return false;
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
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController) {

    }

    @Override
    public void removeAdProviderListener() {

    }

    @Override
    public void skipAd() {

    }

    private static ADConfig parseConfig(Object config) {
        if (config instanceof ADConfig) {
            return ((ADConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), ADConfig.class);
        }
        return null;
    }

    public AdUiListener getAdUiListener() {
         if (adUiListener != null) {
             return  adUiListener;
         }
        adUiListener = new AdUiListener() {
            @Override
            public void adClicked() {

            }

            @Override
            public void onSkip() {

            }

            @Override
            public void onTouch() {

            }

            @Override
            public void onExpand() {

            }

            @Override
            public void onCollapse() {

            }

            @Override
            public void onPause() {

            }

            @Override
            public void onResume() {

            }
        };
        return adUiListener;
    }
}
