package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.ovp.OvpConfigs;
import com.kaltura.playkit.plugins.ads.AdEvent;
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
    private Player player;
    private MessageBus messageBus;
    private PlayerConfig.Media mediaConfig;

    public YouboraLibraryManager(String options) throws JSONException {
        super(options);
    }

    public YouboraLibraryManager(Map<String, Object> options, MessageBus messageBus, PlayerConfig.Media mediaConfig, Player player) {
        super(options);
        this.messageBus = messageBus;
        this.mediaConfig = mediaConfig;
        this.player = player;
        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
        messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
    }

    protected void init() {
        super.init();
        this.pluginName = OvpConfigs.ClientTag;
        this.pluginVersion = "5.3.0-"+ OvpConfigs.ClientTag;
        ViewManager.setMonitoringInterval(MONITORING_INTERVAL);
    }

    private void onEvent(PlayerEvent.StateChanged event) {
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
                    case TRACKS_AVAILABLE:
                        log.d("onEvent: ");
                        break;
                    default:
                        break;
                }
                log.d(event.eventType().name());
                if (((PlayerEvent) event).type != PlayerEvent.Type.STATE_CHANGED){
                    messageBus.post(new LogEvent(TAG + " " + ((PlayerEvent) event).type.toString()));
                }
            } else if (event instanceof AdEvent){
                onAdEvent((AdEvent) event);
            }
        }
    };

    private void onAdEvent(AdEvent event) {
        log.d(event.type.toString());
        switch (event.type) {
            case STARTED:
                ignoringAdHandler();
                break;
            case SKIPPED:
            case COMPLETED:
                ignoredAdHandler();
                break;
            default:
                break;
        }
        log.d(event.type.toString());
        messageBus.post(new LogEvent(TAG + " " + event.type.toString()));
    }

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

    public Double getMediaDuration() {
        return Long.valueOf(mediaConfig.getMediaEntry().getDuration()).doubleValue();
    }

    public String getRendition() {
        return null;
    }

    public String getPlayerVersion() {
        return OvpConfigs.ClientTag;
    }

    public String getResource() {
            return "unknown";
    }

    public Double getPlayhead() {
        return Long.valueOf(player.getCurrentPosition()).doubleValue();
    }


}
