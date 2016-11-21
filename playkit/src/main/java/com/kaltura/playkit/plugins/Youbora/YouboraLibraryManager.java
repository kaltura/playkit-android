package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
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

    public PlayerState.Listener getStateChangeListener(){ return mStateChangeListener;}

    private PlayerState.Listener mStateChangeListener = new PlayerState.Listener() {
        @Override
        public void onPlayerStateChanged(Player player, PlayerState newState) {
            switch (newState){
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
            if (player.getAutoPlay()) {
                resumeHandler();
            } else {
                pauseHandler();
            }
        }
    };

    public PlayerEvent.Listener getEventListener(){
        return mEventListener;
    }

    private PlayerEvent.Listener mEventListener = new PlayerEvent.Listener() {
        @Override
        public void onPlayerEvent(Player player, PlayerEvent event) {
            switch (event){
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

                    break;
                case LOADED_METADATA:

                    break;
                case PAUSE:
                    pauseHandler();
                    break;
                case PLAY:
                    playHandler();
                    break;
                case PLAYING:

                    break;
                case SEEKED:

                    break;
                case SEEKING:

                    break;
                default:

                    break;
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
