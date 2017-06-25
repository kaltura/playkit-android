package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.OttEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.tvpapi.services.MediaMarkService;
import com.kaltura.playkit.plugins.configs.TVPAPIAnalyticsConfig;
import com.kaltura.playkit.utils.Consts;

import java.util.Timer;

/**
 * Created by zivilan on 08/12/2016.
 */

public class TVPAPIAnalyticsPlugin extends PhoenixAnalyticsPlugin {
    private static final PKLog log = PKLog.get("TVPAPIAnalyticsPlugin");
    private long lastKnownPlayerPosition = 0;
    private TVPAPIAnalyticsConfig pluginConfig;
    private String baseUrl;
    private JsonObject initObj;

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
        this.pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        this.initObj = pluginConfig.getInitObj().toJsonObject();
        setPlayer(player);
        setContext(context);
        setTimer(new Timer());
        setRequestsExecutor(APIOkRequestsExecutor.getSingleton());
        setMediaHitInterval((pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH);
        if (baseUrl != null && !baseUrl.isEmpty() &&  initObj != null) {
            messageBus.listen(getEventListener(), PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED);
        } else {
            log.e("Error, base url/initObj - incorrect");
        }
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        setMediaHitInterval((pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH);
        this.initObj = pluginConfig.getInitObj().toJsonObject();
        if (baseUrl == null || baseUrl.isEmpty() || initObj == null) {
            cancelTimer();
            getMessageBus().remove(getEventListener(),(Enum[]) PlayerEvent.Type.values());
        }
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    @Override
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        String fileId = getFileId();
        String action = eventType.name().toLowerCase();
        String method = action.equals("hit")? "MediaHit": "MediaMark";

        if (initObj == null) {
            return;
        }

        if (!"stop".equals(action)) {
            lastKnownPlayerPosition = getPlayer().getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
        }
        RequestBuilder requestBuilder = MediaMarkService.sendTVPAPIEVent(baseUrl + "m=" + method, initObj, action,
                getMediaConfig().getMediaEntry().getId(), fileId, lastKnownPlayerPosition);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getResponse().toLowerCase().contains("concurrent")){
                    getMessageBus().post(new OttEvent(OttEvent.OttEventType.Concurrency));
                    getMessageBus().post(new TVPapiAnalyticsEvent.TVPapiAnalyticsReport(eventType.toString()));
                }
                log.d("onComplete send event: " + eventType);
            }
        });
        getRequestsExecutor().queue(requestBuilder.build());
    }

    private static TVPAPIAnalyticsConfig parseConfig(Object config) {
        if (config instanceof TVPAPIAnalyticsConfig) {
            return ((TVPAPIAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), TVPAPIAnalyticsConfig.class);
        }
        return null;
    }
}
