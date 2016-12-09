package com.kaltura.playkit.addon.cast;

import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;
import com.kaltura.playkit.plugins.ads.ima.IMAPlugin;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itanbarpeled on 07/12/2016.
 */
public class GoogleCastAddOn {


    private static final String MOCK_DATA = "MOCK_DATA";
    private static final String TEMP_LIB_URL = "https://kgit.html5video.org/pulls/3156/";



    // TODO itan - remove this temp method
    public static void getMediaInfoBuilder(OnCompletion<MediaInfo.Builder> completionListener) {

        JSONObject customData = new JSONObject();
        JSONObject embedConfig = new JSONObject();

        try {

            embedConfig.put("lib", TEMP_LIB_URL);
            embedConfig.put("publisherID", "243342");
            embedConfig.put("uiconfID", "21099702");
            embedConfig.put("entryID", "0_l1v5vzh3");

            customData.put("embedConfig", embedConfig);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType("application/x-mpegurl")
                .setCustomData(customData);


        completionListener.onComplete(mediaInfoBuilder);


        /*
        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder("https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/BigBuckBunny.m3u8")

                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("application/x-mpegurl");

        completionListener.onComplete(mediaInfoBuilder);
        */

    }


    public static void getMediaInfoBuilder(PlayerConfig playerConfig, SessionProvider sessionProvider,
                                           final String uiConf, final String fileFormat,
                                           final OnCompletion<MediaInfo.Builder> completionListener) {


        final String entryId = getEntryId(playerConfig);
        final int partnerId = getPartnerId(sessionProvider);
        final String adTagUrl = getAdTagUrl(playerConfig);


        final MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType(MOCK_DATA);


        getKs(sessionProvider, new OnCompletion<String>() {

            @Override
            public void onComplete(final String ks) {

                mediaInfoBuilder.setCustomData(getCustomData(uiConf, fileFormat, entryId, partnerId, adTagUrl, ks));

                completionListener.onComplete(mediaInfoBuilder);


            }
        });

    }



    public static void getMediaInfoBuilder(PlayerConfig playerConfig, SessionProvider sessionProvider,
                                           String uiConf, final OnCompletion<MediaInfo.Builder> completionListener) {

        getMediaInfoBuilder(playerConfig, sessionProvider, uiConf, null, new OnCompletion<MediaInfo.Builder>() {

            @Override
            public void onComplete(MediaInfo.Builder mediaInfoBuilder) {

                completionListener.onComplete(mediaInfoBuilder);

            }

        });

    }



    private static void getKs(SessionProvider sessionProvider, final OnCompletion<String> completionListener) {


        sessionProvider.getKs(new OnCompletion<String>() {

            @Override
            public void onComplete(String ks) {

                completionListener.onComplete(ks);

            }
        });

    }



    private static String getAdTagUrl(PlayerConfig playerConfig) {

        String imaPluginName = IMAPlugin.factory.getName();
        JsonObject imaPlugin = playerConfig.plugins.getPluginConfig(imaPluginName);

        Gson gson = new Gson();
        IMAConfig adConfig = gson.fromJson(imaPlugin, IMAConfig.class);

        return adConfig.getAdTagURL();

    }


    private static String getEntryId(PlayerConfig playerConfig) {

        PKMediaEntry mediaEntry = playerConfig.media.getMediaEntry();

        return mediaEntry.getId();

    }


    private static int getPartnerId(SessionProvider sessionProvider) {

        return sessionProvider.partnerId();

    }




    private static JSONObject getCustomData(String uiConf, String fileFormat, String entryId,
                                            int partnerId, String adTagUrl, String ks) {

        JSONObject customData = new JSONObject();

        try {

            customData.put("embedConfig", getEmbedConfig(uiConf, fileFormat, entryId, partnerId, adTagUrl, ks));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return customData;
    }




    private static JSONObject getEmbedConfig(String uiConf, String fileFormat, String entryId,
                                             int partnerId, String adTagUrl, String ks) {

        JSONObject embedConfig = new JSONObject();

        try {

            embedConfig.put("lib", TEMP_LIB_URL);
            embedConfig.put("publisherID", partnerId);
            embedConfig.put("uiconfID", uiConf);
            embedConfig.put("entryID", entryId);
            //embedConfig.put("flashVars", getFlashVars(ks));
            //setFileFormat(embedConfig, fileFormat);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  embedConfig;

    }


    private static void setFileFormat(JSONObject embedConfig, String fileFormat) {

        try {

            if (!TextUtils.isEmpty(fileFormat)) {
                embedConfig.put("sourceId", fileFormat);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }



    private static JSONObject getFlashVars(String ks) {

        JSONObject flashVars = new JSONObject();

        try {

            flashVars.put("ks", "1234");
            //flashVars.put("proxyData", "initObject");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  flashVars;

    }




}
