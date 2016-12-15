package com.kaltura.playkit.plugins;

import com.google.gson.JsonObject;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.OttEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.backend.tvpapi.MediaMarkService;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.ResponseElement;


/**
 * Created by zivilan on 08/12/2016.
 */

public class TVPAPIAnalyticsPlugin extends PhoenixAnalyticsPlugin {
    private static final PKLog log = PKLog.get("TVPAPIAnalyticsPlugin");
    private static final String TAG = "TVPAPIAnalytics";
    private JsonObject testInitObj = new JsonObject();

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "TVPAPIAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new TVPAPIAnalyticsPlugin();
        }
    };
    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    @Override
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        String fileId = pluginConfig.has("fileId")? pluginConfig.getAsJsonPrimitive("fileId").getAsString():"464302";
        String baseUrl = pluginConfig.has("baseUrl")? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString():"http://tvpapi-preprod.ott.kaltura.com/v3_9/gateways/jsonpostgw.aspx?";
        JsonObject initObj = pluginConfig.has("initObj")? pluginConfig.getAsJsonObject("initObj") : testInitObj;
        String action = eventType.name().toLowerCase();
        String method = action.equals("hit")? "MediaHit": "MediaMark";


        RequestBuilder requestBuilder = MediaMarkService.sendTVPAPIEVent(baseUrl + "m=" + method, initObj.get("initObj").getAsJsonObject(), action,
                 mediaConfig.getMediaEntry().getId(), /*mediaConfig.getMediaEntry().getFileId()*/ fileId, player.getCurrentPosition());

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getResponse().contains("concurrent")){
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                }
                log.d("onComplete send event: ");
                messageBus.post(new LogEvent(TAG + " " + eventType.name()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
