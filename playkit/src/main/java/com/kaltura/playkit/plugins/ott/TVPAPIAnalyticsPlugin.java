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

package com.kaltura.playkit.plugins.ott;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.tvpapi.services.MediaMarkService;
import com.kaltura.playkit.utils.Consts;

import java.util.Timer;

public class TVPAPIAnalyticsPlugin extends PhoenixAnalyticsPlugin {
    private static final PKLog log = PKLog.get("TVPAPIAnalyticsPlugin");
    private JsonObject initObject;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "TVPAPIAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new TVPAPIAnalyticsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");
        setPluginMembers(config);
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;
        this.timer = new Timer();
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        if (baseUrl != null && !baseUrl.isEmpty() &&  initObject != null) {
            messageBus.listen(getEventListener(), PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED);
        } else {
            log.e("Error, base url/initObj - incorrect");
        }
    }

    private void setPluginMembers(Object config) {
        TVPAPIAnalyticsConfig pluginConfig = parseConfig(config);
        if (pluginConfig != null) {
            this.baseUrl = pluginConfig.getBaseUrl();
            this.initObject = pluginConfig.getInitObject();
            long timerInterval = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
            int timerIntervalSec = pluginConfig.getTimerInterval();
            if (timerIntervalSec > 0) {
                timerInterval = timerIntervalSec * Consts.MILLISECONDS_MULTIPLIER;
            }
            this.mediaHitInterval = (int) timerInterval;
        }
    }

    @Override
    protected void onUpdateConfig(Object config) {
        setPluginMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() || initObject == null) {
            cancelTimer();
            messageBus.remove(getEventListener(),(Enum[]) PlayerEvent.Type.values());
        }
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    @Override
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        String method = eventType == PhoenixActionType.HIT ? "MediaHit": "MediaMark";
        String action = eventType.name().toLowerCase();

        if (initObject == null) {
            return;
        }

        if (eventType != PhoenixActionType.STOP) {
            lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
        }
        RequestBuilder requestBuilder = MediaMarkService.sendTVPAPIEvent(baseUrl + "m=" + method, initObject, action,
                mediaConfig.getMediaEntry().getId(), this.fileId, lastKnownPlayerPosition);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getResponse().toLowerCase().contains("concurrent")){
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                    messageBus.post(new TVPAPIAnalyticsEvent.TVPAPIAnalyticsReport(eventType.toString()));
                }
                log.d("onComplete send event: " + eventType);
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private static TVPAPIAnalyticsConfig parseConfig(Object config) {
        if (config instanceof TVPAPIAnalyticsConfig) {
            return ((TVPAPIAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            JsonObject jsonConfig = (JsonObject) config;
            String baseUrl = jsonConfig.get("baseUrl").getAsString();
            int timerInterval = jsonConfig.get("timerInterval").getAsInt();
            JsonObject initObj = jsonConfig.getAsJsonObject("initObj");
            return new TVPAPIAnalyticsConfig(baseUrl, timerInterval, initObj);
        }
        return null;
    }
}
