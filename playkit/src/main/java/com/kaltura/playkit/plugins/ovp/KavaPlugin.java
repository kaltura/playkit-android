package com.kaltura.playkit.plugins.ovp;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by anton.afanasiev on 27/09/2017.
 */

public class KavaPlugin extends PKPlugin {

    private static final PKLog log = PKLog.get(KavaPlugin.class.getSimpleName());
    private static final String DEFAULT_BASE_URL = "";
    private static final long TIMER_INTERVAL = 10000;

    private Player player;
    private PKMediaConfig mediaConfig;
    private MessageBus messageBus;
    private JsonObject pluginConfig;
    private Timer timer = new Timer();
    private RequestQueue requestExecuter;
    private PKEvent.Listener eventListener = initEventListener();


    private int uiconfId;
    private int partnerId;
    private String baseUrl;
    private String ks;




    private enum KavaEvents {
        IMPRESSION(1),
        PLAY_REQUEST(2),
        PLAY(3),
        RESUME(4),
        PLAY_REACHED_25_PERCENT(11),
        PLAY_REACHED_50_PERCENT(12),
        PLAY_REACHED_75_PERCENT(13),
        PLAY_REACHED_100_PERCENT(14),
        PAUSE(33),
        REPLAY(34),
        SEEK(35),
        SOURCE_SELECTED(39),
        VIEW(99);

        private final int value;

        KavaEvents(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }


    @Override
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        this.player = player;
        this.messageBus = messageBus;
        this.pluginConfig = (JsonObject) config;
        this.requestExecuter = APIOkRequestsExecutor.getSingleton();
        this.messageBus.listen(eventListener, (Enum[]) PlayerEvent.Type.values());
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(Object config) {

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


    private PKEvent.Listener initEventListener() {
        return new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if(event instanceof PlayerEvent) {
                    switch (((PlayerEvent) event).type) {
                        case STATE_CHANGED:
                            handleStateChanged((PlayerEvent.StateChanged) event);
                            break;
                        case PLAY:
                            sendAnalyticsEvent(KavaEvents.PLAY);
                            break;
                        case PAUSE:
                            sendAnalyticsEvent(KavaEvents.PAUSE);
                            break;
                        case PLAYING:
                            break;
                        case SEEKED:
                            sendAnalyticsEvent(KavaEvents.SEEK);
                            break;
                        case REPLAY:
                            sendAnalyticsEvent(KavaEvents.REPLAY);
                            break;
                        case SOURCE_SELECTED:
                            sendAnalyticsEvent(KavaEvents.SOURCE_SELECTED);
                            break;
                    }
                }
            }
        };
    }

    private void sendAnalyticsEvent(KavaEvents event) {

    }

    private void handleStateChanged(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case IDLE:
                break;
            case LOADING:
                break;
            case READY:
                startTimerInterval();
                break;
            case BUFFERING:
                break;
        }
    }

    private void startTimerInterval() {
        if(timer == null) {
            timer = new Timer();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                float progress = player.getCurrentPosition() / player.getDuration();
            }
        }, 0, TIMER_INTERVAL);
    }
}
