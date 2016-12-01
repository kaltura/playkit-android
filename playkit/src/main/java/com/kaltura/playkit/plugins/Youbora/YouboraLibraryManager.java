package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.ovp.OvpConfigs;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.managers.ViewManager;

import org.json.JSONException;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraLibraryManager extends PluginGeneric {
    private static final PKLog log = PKLog.get("YouboraLibraryManager");
    private static final String TAG = "YouboraPlugin";

    private Double lastReportedBitrate = super.getBitrate();
    private Double lastReportedthroughput = super.getThroughput();
    private static final long MONITORING_INTERVAL = 200L;
    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private MessageBus messageBus;

    public YouboraLibraryManager(String options) throws JSONException {
        super(options);

    }

    public YouboraLibraryManager(Map<String, Object> options, MessageBus messageBus) {
        super(options);
        this.messageBus = messageBus;
    }

    protected void init() {
        super.init();
        this.pluginName = OvpConfigs.ClientTag;
        this.pluginVersion = "5.3.0-"+ OvpConfigs.ClientTag;
        ViewManager.setMonitoringInterval(MONITORING_INTERVAL);
    }

    public void onEvent(PlayerEvent.StateChanged event) {
        log.d(event.newState.toString());
        switch (event.newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    bufferedHandler();
                }
                break;
            case BUFFERING:
                isBuffering = true;
                bufferingHandler();
                break;
            default:
                break;
        }
        log.d(event.newState.toString());
        messageBus.post(new LogEvent(TAG + " " + event.newState.toString()));
    }

    public PKEvent.Listener getEventListener() {
        return mEventListener;
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                log.d(((PlayerEvent) event).type.toString());
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        YouboraLibraryManager.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case ENDED:
                        endedHandler();
                        break;
                    case ERROR:
                        errorHandler(event.eventType().toString());
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
                log.d(event.eventType().name());
                if (((PlayerEvent) event).type != PlayerEvent.Type.STATE_CHANGED){
                    messageBus.post(new LogEvent(TAG + " " + ((PlayerEvent) event).type.toString()));
                }
            }
        }
    };

    public void startMonitoring(Object player) {
        log.d("startMonitoring");
        super.startMonitoring(player);
        this.lastReportedBitrate = super.getBitrate();
        this.enableSeekMonitor();
    }

    public void stopMonitoring() {
        log.d("stopMonitoring");
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
