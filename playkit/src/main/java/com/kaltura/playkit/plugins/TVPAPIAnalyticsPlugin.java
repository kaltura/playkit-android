package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.OttEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.api.tvpapi.services.MediaMarkService;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by zivilan on 08/12/2016.
 */

public class TVPAPIAnalyticsPlugin extends PhoenixAnalyticsPlugin {
    private static final PKLog log = PKLog.get("TVPAPIAnalyticsPlugin");

    private JsonObject testInitObj = new JsonObject();
    private long lastKnownPlayerPosition = 0;

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
    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    @Override
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        JsonObject initObj = pluginConfig.has("initObj")? pluginConfig.getAsJsonObject("initObj") : testInitObj;
        String action = eventType.name().toLowerCase();
        String method = action.equals("hit")? "MediaHit": "MediaMark";

        if (initObj == null) {
            return;
        }
        if (!"stop".equals(action)) {
            lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
        }
        RequestBuilder requestBuilder = MediaMarkService.sendTVPAPIEVent(getBaseUrl() + "m=" + method, initObj, action,
                mediaConfig.getMediaEntry().getId(), getFileId(), lastKnownPlayerPosition);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getResponse().toLowerCase().contains("concurrent")){
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                    messageBus.post(new TVPapiAnalyticsEvent.TVPapiAnalyticsReport(eventType.toString()));
                }
                log.d("onComplete send event: " + eventType);
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
