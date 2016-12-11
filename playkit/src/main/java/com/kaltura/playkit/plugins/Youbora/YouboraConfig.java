package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by zivilan on 17/11/2016.
 */

public class YouboraConfig {
    private static final PKLog log = PKLog.get("YouboraConfig");

    private static Map<String, Object> youboraConfig = null;
    private static final Map<String, Object> defaultYouboraConfig;
    private static final Map<String, Object> mediaObject;
    private static final Map<String, Object> youboraConfigObject;
    private static final Map<String, Object> propertiesObject;
    private static final Map<String, Object> extraParamsObject;
    private static final Map<String, Object> adsObject;
    private static final Map<String, Object> networkObject;

    static {
        HashMap<String, Object> youboraConfig = new HashMap<>(20);
        youboraConfig.put("enableAnalytics", true);
        youboraConfig.put("parseHLS", false);
        youboraConfig.put("parseCDNNodeHost", false);
        youboraConfig.put("hashTitle", true);
        youboraConfig.put("httpSecure", false);
        youboraConfig.put("enableNiceBuffer", true);
        youboraConfig.put("enableNiceSeek", true);
        youboraConfig.put("accountCode", "kalturatest");
        youboraConfig.put("username", "");
        youboraConfig.put("transactionCode", "");
        youboraConfig.put("isBalanced", "0");
        youboraConfig.put("isResumed", "0");
        youboraConfig.put("haltOnError", true);
        youboraConfigObject = youboraConfig;

        Map<String, Object> network = new HashMap<>(2);
        network.put("ip", "");
        network.put("isp", "");
        youboraConfig.put("network", network);
        networkObject = network;

        Map<String, Object> device = new HashMap<>(1);
        device.put("id", null);
        youboraConfig.put("device", device);

        Map<String, Object> media = new HashMap<>(5);
        media.put("isLive", false);
        media.put("resource", null);
        media.put("title", "Title");
        media.put("duration", null);
        media.put("cdn", null);
        youboraConfig.put("media", media);
        mediaObject = media;

        Map<String, Object> ads = new HashMap<>(6);
        ads.put("adsExpected", false);
        ads.put("resource", null);
        ads.put("campaign", "");
        ads.put("title", null);
        ads.put("position", null);
        ads.put("duration", null);
        youboraConfig.put("ads", ads);
        adsObject = ads;


        Map<String, Object> properties = new HashMap<>(16);
        properties.put("contentId", null);
        properties.put("type", "video");
        properties.put("transaction_type", null);
        properties.put("genre", "Action");
        properties.put("language", null);
        properties.put("year", "");
        properties.put("cast", null);
        properties.put("director", null);
        properties.put("owner", null);
        properties.put("parental", null);
        properties.put("price", null);
        properties.put("rating", null);
        properties.put("audioType", null);
        properties.put("audioChannels", null);
        properties.put("device", null);
        properties.put("quality", null);
        youboraConfig.put("properties", properties);
        propertiesObject = properties;

        Map<String, Object> extraParams = new HashMap<>(10);
        extraParams.put("param1", PlayKitManager.CLIENT_TAG);
        extraParams.put("param2", "Param 2 value");
        extraParams.put("param3", "Param 3 value");
        extraParams.put("param4", "Param 4 value");
        extraParams.put("param5", "Param 5 value");
        extraParams.put("param6", "Param 6 value");
        extraParams.put("param7", "Param 7 value");
        extraParams.put("param8", "Param 8 value");
        extraParams.put("param9", "Param 9 value");
        extraParams.put("param10", "Param 10 value");
        youboraConfig.put("extraParams", extraParams);
        extraParamsObject = extraParams;

        defaultYouboraConfig = Collections.unmodifiableMap(youboraConfig);
    }

    private YouboraConfig() {}

    public static void saveYouboraConfig(Context context, Map<String, Object> config){
        youboraConfig = config;
        // save to file
        File file = new File(context.getFilesDir(), "youbora_config_map");

        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(youboraConfig);
            outputStream.flush();
            outputStream.close();
            Log.i("YouboraConfigManager", "saved youbora config to file");
        } catch (Exception e) {
            Log.wtf("YouboraConfigManager", "exception when saving config to file: " + e.toString());
        }
    }

    public static Map<String, Object> getYouboraConfig(JsonObject pluginConfig, PlayerConfig.Media mediaConfig) {
        if (youboraConfig == null) {
            // load from json
            setYouboraConfig(pluginConfig, mediaConfig);
        }
        return youboraConfig;
    }

    private static void setYouboraConfig(JsonObject pluginConfig, PlayerConfig.Media mediaConfig){
        youboraConfig = defaultYouboraConfig;
        if (mediaConfig != null) {
            mediaObject.put("resource", mediaConfig.getMediaEntry().getId());
            mediaObject.put("title", mediaConfig.getMediaEntry().getId()); //name?
            mediaObject.put("duration", mediaConfig.getMediaEntry().getDuration());
        }
        if (pluginConfig != null){
            if (pluginConfig.has("title")){
                mediaObject.put("title",pluginConfig.getAsJsonPrimitive("title").getAsString());
            }
            if (pluginConfig.has("genre")){
                propertiesObject.put("genre",pluginConfig.getAsJsonPrimitive("genre").getAsString());
            }
            if (pluginConfig.has("param1")){
                extraParamsObject.put("param1",pluginConfig.getAsJsonPrimitive("param1").getAsString());
            }
            if (pluginConfig.has("adsAnalytics")){
                adsObject.put("adsExpected", pluginConfig.getAsJsonPrimitive("adsAnalytics").getAsBoolean());
            }
        }
    }

    public static Map<String, Object> resetPreferences(Context context) {
        youboraConfig = new LinkedHashMap<>(defaultYouboraConfig);
        saveYouboraConfig(context, youboraConfig);
        return youboraConfig;
    }

    public static Map<String, Object> getDefaultPreferences() {
        // default config
        return defaultYouboraConfig;
    }
}
