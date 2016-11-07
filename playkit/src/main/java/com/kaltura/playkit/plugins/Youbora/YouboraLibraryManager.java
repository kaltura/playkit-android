package com.kaltura.playkit.plugins.Youbora;

import android.text.TextUtils;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.TVPAPIAnalyticsPlugin;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.managers.ViewManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
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

    private void setMessageParams(TVPAPIAnalyticsPlugin.TVPAPIEventType eventType, String eventContent){
        JSONObject baseParams = getBaseParams();
        try {
            baseParams.put("Action", eventContent);
            baseParams.put("MethodName", eventType.toString());
            sendMessage(eventType,baseParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getBaseParams(){
        int mediaType = 0;
        JSONObject postData = new JSONObject();
        try {
            postData.put("initObj", mPlayerConfig.getInitObject());
            postData.put("mediaType", mediaType);
            postData.put("iMediaID", mPlayerConfig.getMediaEntry().getId());
            postData.put("iFileID", mFileId);
            postData.put("iLocation", mPlayer.getCurrentPosition());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return postData;
    }

    private String sendMessage(TVPAPIAnalyticsPlugin.TVPAPIEventType service, JSONObject postData){
        URL url = mPlayerConfig.getApiBaseUrl();
        if (service != null && postData != null) {
            String messageUrl = buildUrl(service.toString(), postData);
            return messageUrl;
        } else {
            return "";
        }
    }

    private static String buildUrl(String original, JSONObject postData) {
        if (postData != null) {
            String methodName = postData.optString("MethodName");
            if (!TextUtils.isEmpty(methodName)) {
                postData.remove("MethodName");
                return original.concat(methodName);
            }
        }
        return original;
    }

}
