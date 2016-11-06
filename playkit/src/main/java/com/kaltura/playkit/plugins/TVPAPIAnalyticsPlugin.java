package com.kaltura.playkit.plugins;

import android.content.Context;
import android.text.TextUtils;

import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import static com.kaltura.playkit.plugins.TVPAPIAnalyticsPlugin.TVPAPIEventType.MEDIAMARK;
import static com.kaltura.playkit.plugins.TVPAPIAnalyticsPlugin.TVPAPIEventType.MEDIA_MARK;

/**
 * Created by zivilan on 02/11/2016.
 */

public class TVPAPIAnalyticsPlugin extends PKPlugin {
    enum TVPAPIEventType{
        MEDIAMARK,
        MEDIAHIT;
    }
    private boolean mIsPlaying = false;
    private boolean mIsConcurrent = false;
    private boolean mDidFirstPlay = false;
    private int mMediaHitInterval = -1;
    private int mFileId = -1;
    private long mContinueTime;
    private PlayerConfig mPlayerConfig;
    private Context mContext;
    private Player mPlayer;
    private boolean mPlayFromContinue = false;


    private static final String TAG = "TVPAPIAnalytics";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "TVPAPIAnalytics";
        }

        @Override
        public PKPlugin newInstance(PlayKit playKitManager) {
            return new TVPAPIAnalyticsPlugin();
        }
    };

    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        player.addEventListener(mEventListener);
        this.mPlayerConfig = playerConfig;
        this.mContext = context;
        this.mPlayer = player;
        if (mPlayerConfig.getStartPosition() != -1){
            this.mContinueTime = mPlayerConfig.getStartPosition();
            this.mPlayFromContinue = true;
        }
    }

    @Override
    public void release() {

    }

    private PlayerEvent.Listener mEventListener = new PlayerEvent.Listener() {
        @Override
        public void onPlayerEvent(Player player, PlayerEvent event) {
            switch (event){
                case CAN_PLAY:
                    mDidFirstPlay = false;
//                    mFileId = mPlayerConfig.getFileId();
                    break;
                case DURATION_CHANGE:

                    break;
                case ENDED:
                    mIsPlaying = false;
                    setMessageParams(MEDIAMARK, "finish");
                    break;
                case ERROR:

                    break;
                case LOADED_METADATA:
                    setMessageParams(MEDIAMARK, "load");
                    break;
                case PAUSE:
                    mIsPlaying = false;
                    if (mDidFirstPlay){
                        setMessageParams(MEDIAMARK, "pause");
                    }
                    break;
                case PLAY:
                    if (!mDidFirstPlay){
                        mDidFirstPlay = true;
                        mIsPlaying = true;
                        setMessageParams(MEDIAMARK, "first_play");
                    } else {
                        mIsPlaying = true;
                        startMediaHitInterval();
                        setMessageParams(MEDIAMARK, "play");
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
    };

    private void bindContinueToTime(){

    }

    private void startMediaHitInterval(){

    }

    private void setMessageParams(TVPAPIEventType eventType, String eventContent){
        JSONObject baseParams = getBaseParams();
        try {
            baseParams.put("Action", eventContent);
            baseParams.put("MethodName", eventType.toString())
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

    private String sendMessage(TVPAPIEventType service, JSONObject postData){
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
