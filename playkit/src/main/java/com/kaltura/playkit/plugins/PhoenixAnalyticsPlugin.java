package com.kaltura.playkit.plugins;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zivilan on 02/11/2016.
 */

public class PhoenixAnalyticsPlugin extends PKPlugin {
    enum PhoenixActionType{
        HIT,
        PLAY,
        STOP,
        PAUSE,
        FIRST_PLAY,
        SWOOSH,
        LOAD,
        FINISH,
        BITRATE_CHANGE,
        ERROR
    }
    private boolean mIsPlaying = false;
    private boolean mIsConcurrent = false;
    private boolean mDidFirstPlay = false;
    private int mMediaHitInterval = -1;
    private int mFileId = -1;
    private long mContinueTime;
    private PlayerConfig.Media mMediaConfig;
    private JsonObject mPluginConfig;
    private Context mContext;
    private Player mPlayer;
    private boolean mPlayFromContinue = false;


    private static final String TAG = "PhoenixAnalytics";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "PhoenixAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new PhoenixAnalyticsPlugin();
        }
    };

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mMediaConfig = mediaConfig;
        this.mPlayer = player;
        this.mPluginConfig = pluginConfig;
        this.mContext = context;
        messageBus.listen(mEventListener);
        if (mMediaConfig.getStartPosition() != -1){
            this.mContinueTime = mMediaConfig.getStartPosition();
            this.mPlayFromContinue = true;
        }
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch ((PlayerEvent) event) {
                    case CAN_PLAY:
                        mDidFirstPlay = false;
//                    mFileId = mPlayerConfig.getFileId();
                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:
                        mIsPlaying = false;
                        setMessageParams(PhoenixActionType.FINISH);
                        break;
                    case ERROR:
                        setMessageParams(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        setMessageParams(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        mIsPlaying = false;
                        if (mDidFirstPlay) {
                            setMessageParams(PhoenixActionType.PAUSE);
                        }
                        break;
                    case PLAY:
                        if (!mDidFirstPlay) {
                            mDidFirstPlay = true;
                            mIsPlaying = true;
                            setMessageParams(PhoenixActionType.FIRST_PLAY);
                        } else {
                            mIsPlaying = true;
                            startMediaHitInterval();
                            setMessageParams(PhoenixActionType.PLAY);
                        }

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

    private void bindContinueToTime(){

    }

    private void startMediaHitInterval(){

    }

    private void setMessageParams(PhoenixActionType eventType){

        JSONObject baseParams = getBaseParams();
        try {
            baseParams.put("Action", eventType);
            sendMessage(eventType,baseParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getBaseParams(){
        int mediaType = 0;
        JSONObject postData = new JSONObject();
        try {
//            postData.put("initObj", mPlayerConfig.getInitObject());
            postData.put("mediaType", mediaType);
            postData.put("iMediaID", mMediaConfig.getMediaEntry().getId());
            postData.put("iFileID", mFileId);
            postData.put("iLocation", mPlayer.getCurrentPosition());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return postData;
    }

    private String sendMessage(PhoenixActionType service, JSONObject postData){
//        URL url = mPlayerConfig.getApiBaseUrl();
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
