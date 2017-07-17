/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerDecorator;

/**
 * @hide
 */

public class SamplePlugin extends PKPlugin {

    private static final String TAG = "SamplePlugin";
    private static final PKLog log = PKLog.get("SamplePlugin");

    private Player player;
    private Context context;
    private int delay;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Sample";
        }

        @Override
        public PKPlugin newInstance() {
            return new SamplePlugin();
        }

        @Override
        public void warmUp(Context context) {
            
        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.i("Loading");
        this.player = player;
        this.context = context;
        delay = ((JsonObject) config).getAsJsonPrimitive("delay").getAsInt();
        log.v("delay=" + delay);
        
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("onEvent: " + event);
            }
        });
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
    public void onDestroy() {

    }

    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new PlayerDecorator() {
            @Override
            public void play() {
                super.play();
            }
        };
    }
}
