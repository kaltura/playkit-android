package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PlayerEvent;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.managers.ViewManager;

import org.json.JSONException;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraLibraryManager extends PluginGeneric {
    private Double lastReportedBitrate = super.getBitrate();
    private Double lastReportedthroughput = super.getThroughput();
    private static final long MONITORING_INTERVAL = 200L;
    private boolean isFirstPlay = true;

    public YouboraLibraryManager(String options) throws JSONException {
        super(options);
    }

    public YouboraLibraryManager(Map<String, Object> options) {
        super(options);
    }

    protected void init() {
        super.init();
        this.pluginName = "YouboraPlugin";
        ViewManager.setMonitoringInterval(MONITORING_INTERVAL);
    }


    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case IDLE:

                break;
            case LOADING:

                break;
            case READY:
                playHandler();
                joinHandler();
                bufferedHandler();
                break;
            case BUFFERING:
                bufferingHandler();
                break;
        }
    }

    public PKEvent.Listener getEventListener() {
        return mEventListener;
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        YouboraLibraryManager.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case CAN_PLAY:
                        playHandler();
                        joinHandler();
                        bufferedHandler();
                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:
                        endedHandler();
                        break;
                    case ERROR:
                        errorHandler(event.eventType().toString());
                        break;
                    case LOADED_METADATA:

                        break;
                    case PAUSE:
                        pauseHandler();
                        break;
                    case PLAY:
                        if (!isFirstPlay) {
                            resumeHandler();
                        } else {
                            isFirstPlay = false;
                            playHandler();
                        }
                        break;
                    case PLAYING:
                        playingHandler();
                        break;
                    case SEEKED:
                        seekedHandler();
                        break;
                    case SEEKING:
                        seekingHandler();
                        break;
                    default:

                        break;
                }
            }
        }
    };

    public void startMonitoring(Object player) {
        super.startMonitoring(player);
        this.lastReportedBitrate = super.getBitrate();
        this.enableSeekMonitor();
    }

    public void stopMonitoring() {
        super.stopMonitoring();
    }

    public void setBitrate(Double bitrate) {
        this.lastReportedBitrate = bitrate;
    }

    public void setThroughput(Double throughput) {
        this.lastReportedthroughput = throughput;
    }

    public Double getBitrate() {
        return this.lastReportedBitrate;
    }

    public Double getThroughput() {
        return this.lastReportedthroughput;
    }

}
