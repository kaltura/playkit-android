package com.kaltura.playkit.plugins;

import android.content.Context;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;

import org.json.JSONObject;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaStatisticsPlugin extends PKPlugin {
    private static final String TAG = "KalturaStatisticsPlugin";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaStatistics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaStatisticsPlugin();
        }
    };

    @Override
    protected void load(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, MessageBus messageBus, Context context) {
        player.addEventListener(mEventListener);
    }

    @Override
    public void release() {

    }

    @Override
    protected void update(PlayerConfig playerConfig) {

    }

    private PlayerEvent.Listener mEventListener = new PlayerEvent.Listener() {
        @Override
        public void onPlayerEvent(Player player, PlayerEvent event) {
            switch (event){
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
    };

}
