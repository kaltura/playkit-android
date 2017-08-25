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
import com.kaltura.admanager.ContentProgressProvider;
import com.kaltura.admanager.DefaultAdManager;
import com.kaltura.admanager.DefaultAdUIController;
import com.kaltura.admanager.DefaultStringFetcher;
import com.kaltura.admanager.DefaultUrlPinger;
import com.kaltura.admanager.VideoProgressUpdate;
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
import com.kaltura.playkit.plugins.ads.kaltura.events.AdPluginErrorEvent;

import java.util.List;

import static com.google.obf.hi.b.adsManager;

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


    //------------------------------

    private boolean isAppInBackground = false;
    private boolean isContentPrepared = false;
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

        AdUIController.Factory adUIFactory =  new AdUIController.Factory() {
            @Override
            public AdUIController newAdController(AdUiListener listener, AdPlayer adPlayer) {
                adUIController = new DefaultAdUIController(context, listener, adConfig.getAdSkinContainer());
                return adUIController;
            }
        };
        ContentProgressProvider contentProgressProvider = new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (adsManager == null || player == null) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();

                if (isAdDisplayed || currentPosition < 0 || duration <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(currentPosition, duration);
            }
        };

        adManager = new DefaultAdManager(context, adPlayerFactory, contentProgressProvider, adUIFactory, stringFetcher, urlPinger, null/*Settings*/);
        adManager.addListener(getAdManagerListener());
    }

    public AdManager.Listener getAdManagerListener() {
        if (adManagerListener != null) {
            return adManagerListener;
        }

        adManagerListener = new AdManager.Listener() {

            @Override
            public void onAdEvent(AdEvent.AdEventType adEventType, AdInfo adInfo) {
                log.d("recieved event " + adEventType + " isAppInBackground = " + isAppInBackground);
                switch(adEventType) {
                    case adBreakPending:

                        isAdRequested = true;
                        if (!isAppInBackground) {
                            adManager.playAdBreak();
                        }
                        break;
                    case adBreakStarted:
                        if (!isAppInBackground) {
                            log.d("XXX  PREPAREErecieved event " + adEventType + " isAppInBackground = " + isAppInBackground);
                            preparePlayer(false);
                        }
                        break;
                    case allAdsCompleted:
                        isAllAdsCompleted = true;
                        break;
                    case adProgress:
                        break;
                    case adClicked:
                        break;
                    case adTouched:
                        break;
                    case adPaused:
                        isAdIsPaused = true;
                        break;
                    case adResumed:
                        isAdIsPaused = false;
                        if (!isAppInBackground) {
                            adConfig.getAdSkinContainer().setVisibility(View.VISIBLE);
                        }
                        break;
                    case adSkipped:
                        isAdDisplayed = false;
                        break;
                    case adLoaded:
                        break;
                    case adStarted:
                        isAdDisplayed = true;
                        isAdIsPaused = false;
                        if (!isAppInBackground) {
                            adConfig.getAdSkinContainer().setVisibility(View.VISIBLE);
                        }
                        break;
                    case adEnded:
                        isAdDisplayed = false;
                        adConfig.getAdSkinContainer().setVisibility(View.INVISIBLE);
                        break;
                    case timeSpentViewing:
                        break;
                    case adBufferStart:
                        break;
                    case adBufferEnd:
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onAdBreakEndedEvent(AdBreakEndedReason adBreakEndedReason) {
                log.d("recieved event onAdBreakEnded");
                log.d("XXX remove adplayer");
                removeAdPlayer();
                if (!isContentPrepared) {
                    preparePlayer(true);
                } else {
                    player.play();
                }
            }

            @Override
            public void onAdProgressEvent(AdInfo adInfo, long currentPosition, long adPlaybackPosition) {

            }

            @Override
            public void onAdErrorEvent(AdEvent.AdErrorEventType adErrorEventType, Exception ex) {
                log.e("onAdErrorEvent " + adErrorEventType.name());
                String msg = (ex == null) ? "No error message" : ex.getMessage();
                sendError(adErrorEventType, msg, ex);
                isAdRequested = true;
                isAdError = true;
                removeAdPlayer();
                preparePlayer(true);
                //TODO Check if fatal error and clear all the adBreaks
            }
        };
        return adManagerListener;
    }

    //-----------------------------


    private void sendError(AdEvent.AdErrorEventType errorEventType , String message, Throwable cause) {
        log.e("Ad Error: " + errorEventType.name() + " with message " + message);
        AdPluginErrorEvent.AdErrorEvent errorEvent = new AdPluginErrorEvent.AdErrorEvent(errorEventType, message, cause);
        messageBus.post(errorEvent);
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("Start onUpdateMedia");
        this.mediaConfig = mediaConfig;
        isAdRequested = false;
        isAdDisplayed = false;
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
        isAdRequested = false;
        isAdDisplayed = false;
        isAdIsPaused  = false;
        isAdError     = false;
        isAllAdsCompleted = false;
    }

    @Override
    protected void onApplicationPaused() {
        isAppInBackground = true;
        if (adPlayer != null) {
            if (isAdDisplayed()) {
                adPlayer.onApplicationPaused();
            }
        }
    }

    @Override
    protected void onApplicationResumed() {
        isAppInBackground = false;
        if (adPlayer != null) {
            if (isAdDisplayed()) {
                adPlayer.onApplicationResumed();
            }
        }
    }

    @Override
    protected void onDestroy() {

    }


// ------------------------------------------

    @Override
    public ADConfig getAdsConfig() {
        return adConfig;
    }

    @Override
    public void start() {

    }

    @Override
    public void destroyAdsManager() {

    }

    @Override
    public void resume() {
        adPlayer.play();
    }

    @Override
    public void pause() {
        adPlayer.pause();
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
        return adPlayer != null ? (long) adPlayer.getDurationSec() / 1000 : 0;
    }

    @Override
    public long getCurrentPosition() {
        return adPlayer != null ? (long) adPlayer.getPositionSec() / 1000 : 0;
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

    private static ADConfig parseConfig(Object config) {
        if (config instanceof ADConfig) {
            return ((ADConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), ADConfig.class);
        }
        return null;
    }

//   public AdUiListener getAdUiListener() {
//        if (adUiListener != null) {
//            return  adUiListener;
//        }
//        adUiListener = new AdUiListener() {
//            @Override
//            public void onAdClicked() {
//
//            }
//
//            @Override
//            public void onSkip() {
//
//            }
//
//            @Override
//            public void onTouch() {
//
//            }
//
//            @Override
//            public void onExpand() {
//
//            }
//
//            @Override
//            public void onCollapse() {
//
//            }
//
//            @Override
//            public void onPause() {
//
//            }
//
//            @Override
//            public void onResume() {
//
//            }
//        };
//        return adUiListener;
//    }

    private void preparePlayer(boolean doPlay) {
        log.d("XX AdPlugin prepare");
        if (pkAdProviderListener != null && !isAppInBackground) {
            log.d("XX AdPlugin in prepare player");
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
        adConfig.getAdSkinContainer().setVisibility(View.INVISIBLE);
        if (adPlayer != null) {
            adPlayer.destroy();
            adPlayer = null;
        }
    }
}
