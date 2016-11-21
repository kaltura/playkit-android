package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatisticsPlugin extends PKPlugin {
    private static final String TAG = "KalturaLiveStatisticsPlugin";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStatistics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatisticsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        player.addEventListener(mEventListener);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch ((PlayerEvent) event) {
                    case CAN_PLAY:

                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:

                        break;
                    case ERROR:

                        break;
                    case LOADED_METADATA:

                        break;
                    case PAUSE:

                        break;
                    case PLAY:

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
        }
    };
}
