package com.kaltura.playkit.plugins.Youbora;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;

import java.util.Collections;
import java.util.HashMap;
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

    private static String[] youboraConfigFieldNames = new String[]{"accountCode","username"};
    private static String[] youboraBooleanConfigFieldNames = new String[]{"haltOnError","enableAnalytics"};

    private static String[] mediaConfigFieldNames = new String[]{"title","cdn"};
    private static String[] mediaBooleanConfigFieldNames = new String[]{"isLive"};

    private static String[] adsConfigFieldNames = new String[]{"title","campaign"};
    private static String[] adsBooleanConfigFieldNames = new String[]{"adsExpected"};

    private static String[] propertiesConfigFieldNames = new String[]{"genre","type","transaction_type","year","cast","director","owner","parental","price","rating","audioType","audioChannels"
                                            ,"device","quality"};
    private static String[] extraConfigFieldNames = new String[]{"param2","param3","param4","param5","param6","param7","param8","param9","param10"};

    static {
        HashMap<String, Object> youboraLocalConfig = new HashMap<>(20);
        youboraLocalConfig.put("enableAnalytics", true);
        youboraLocalConfig.put("parseHLS", false);
        youboraLocalConfig.put("parseCDNNodeHost", false);
        youboraLocalConfig.put("hashTitle", true);
        youboraLocalConfig.put("httpSecure", false);
        youboraLocalConfig.put("enableNiceBuffer", true);
        youboraLocalConfig.put("enableNiceSeek", true);
        youboraLocalConfig.put("accountCode", "kalturatest");
        youboraLocalConfig.put("transactionCode", "");
        youboraLocalConfig.put("isBalanced", "0");
        youboraLocalConfig.put("isResumed", "0");
        youboraLocalConfig.put("haltOnError", true);
        youboraConfigObject = youboraLocalConfig;

        Map<String, Object> network = new HashMap<>(2);
        network.put("ip", "");
        network.put("isp", "");
        youboraLocalConfig.put("network", network);
        networkObject = network;

        Map<String, Object> device = new HashMap<>(1);
        device.put("id", null);
        youboraLocalConfig.put("device", device);

        Map<String, Object> media = new HashMap<>(5);
        media.put("isLive", false);
        media.put("cdn", null);
        youboraLocalConfig.put("media", media);
        mediaObject = media;

        Map<String, Object> ads = new HashMap<>(6);
        ads.put("adsExpected", false);
        ads.put("resource", null);
        ads.put("position", null);
        ads.put("duration", null);
        youboraLocalConfig.put("ads", ads);
        adsObject = ads;


        Map<String, Object> properties = new HashMap<>(16);
        youboraLocalConfig.put("properties", properties);
        propertiesObject = properties;

        Map<String, Object> extraParams = new HashMap<>(10);
        extraParams.put("param1", PlayKitManager.CLIENT_TAG);
        youboraLocalConfig.put("extraParams", extraParams);
        extraParamsObject = extraParams;

        defaultYouboraConfig = Collections.unmodifiableMap(youboraLocalConfig);
    }

    private YouboraConfig() {}

    public static Map<String, Object> getYouboraConfig(JsonObject settings, PKMediaConfig mediaConfig, Player player) {
        // load from json
        setYouboraConfig(settings, mediaConfig, player);

        return youboraConfig;
    }

    private static void setYouboraConfig(JsonObject settings, PKMediaConfig mediaConfig, Player player){
        youboraConfig = defaultYouboraConfig;
        if (mediaConfig != null) {
            mediaObject.put("resource", !mediaConfig.getMediaEntry().getSources().isEmpty()? mediaConfig.getMediaEntry().getSources().get(0).getUrl():"");
            Long duration = player.getDuration() / 1000;
            mediaObject.put("duration", duration.intValue()); //Duration should be sent in secs
        }
        if (settings != null){
            if (settings.has("youboraConfig")){
                setYouboraConfigObject(youboraConfigObject, settings.getAsJsonObject("youboraConfig"), youboraConfigFieldNames, youboraBooleanConfigFieldNames);
            }
            if (settings.has("media")){
                setYouboraConfigObject(mediaObject, settings.getAsJsonObject("media"), mediaConfigFieldNames, mediaBooleanConfigFieldNames);
            }
            if (settings.has("ads")){
                setYouboraConfigObject(adsObject, settings.getAsJsonObject("ads"), adsConfigFieldNames, adsBooleanConfigFieldNames);
            }
            if (settings.has("properties")){
                setYouboraConfigObject(propertiesObject, settings.getAsJsonObject("properties"), propertiesConfigFieldNames, null);
            }
            if (settings.has("extraParams")){
                setYouboraConfigObject(extraParamsObject, settings.getAsJsonObject("extraParams"), extraConfigFieldNames, null);
            }
        }
    }

    private static void setYouboraConfigObject(Map<String, Object> defaultJsonObject, JsonObject jsonObject, String[] fieldNames, String[] booleanFieldNames){
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (jsonObject.has(fieldName)) {
                log.d("setYouboraConfigObject: " + fieldNames[i]);
                defaultJsonObject.put(fieldName, jsonObject.getAsJsonPrimitive(fieldName).getAsString());
            }
        }
        if (booleanFieldNames != null) {
            for (int i = 0; i < booleanFieldNames.length; i++) {
                String fieldName = booleanFieldNames[i];
                if (jsonObject.has(fieldName)) {
                    log.d( "setYouboraConfigObject: " + booleanFieldNames[i]);
                    defaultJsonObject.put(fieldName, jsonObject.getAsJsonPrimitive(fieldName).getAsBoolean());
                }
            }
        }
    }
}
