package com.kaltura.playkit.plugins.Youbora;

import android.net.Uri;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.SourceSelector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

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

    private static String[] youboraConfigFieldNames = new String[]{"accountCode","username"};
    private static String[] youboraBooleanConfigFieldNames = new String[]{"haltOnError","enableAnalytics"};

    private static String[] mediaConfigFieldNames = new String[]{"title","cdn"};
    private static String[] mediaBooleanConfigFieldNames = new String[]{"isLive"};

    private static String[] adsConfigFieldNames = new String[]{"title","campaign"};
    private static String[] adsBooleanConfigFieldNames = new String[]{};

    private static String[] propertiesConfigFieldNames = new String[]{"genre","type","transaction_type","year","cast","director","owner","parental","price","rating","audioType","audioChannels"
                                            ,"device","quality"};
    private static String[] extraConfigFieldNames = new String[]{"param1","param2","param3","param4","param5","param6","param7","param8","param9","param10"};

    static {
        HashMap<String, Object> youboraLocalConfig = new HashMap<>(20);
        youboraLocalConfig.put("enableAnalytics", true);
        youboraLocalConfig.put("parseHLS", false);
        youboraLocalConfig.put("parseCDNNodeHost", false);
        youboraLocalConfig.put("httpSecure", false);
        youboraLocalConfig.put("accountCode", "kalturatest");
        youboraLocalConfig.put("transactionCode", "");
        youboraLocalConfig.put("haltOnError", true);
        
        youboraConfigObject = youboraLocalConfig;

        Map<String, Object> device = new HashMap<>(1);
        device.put("id", null);

        Map<String, Object> media = new HashMap<>(5);
        media.put("cdn", null);
        youboraLocalConfig.put("media", media);
        mediaObject = media;

        Map<String, Object> ads = new HashMap<>(6);
        ads.put("resource", null);
        ads.put("position", null);
        ads.put("duration", null);
        youboraLocalConfig.put("ads", ads);
        adsObject = ads;


        Map<String, Object> properties = new HashMap<>(16);
        youboraLocalConfig.put("properties", properties);
        propertiesObject = properties;

        Map<String, Object> extraParams = new HashMap<>(10);
        youboraLocalConfig.put("extraParams", extraParams);
        extraParamsObject = extraParams;

        defaultYouboraConfig = Collections.unmodifiableMap(youboraLocalConfig);
    }

    private YouboraConfig() {}

    public static Map<String, Object> getYouboraConfig(JsonObject pluginConfig, PKMediaConfig mediaConfig, Player player) {
        // load from json
        setYouboraConfig(pluginConfig, mediaConfig, player);

        return youboraConfig;
    }

    public static Map<String, Object> setYouboraAdsConfig(JsonObject pluginConfig) {
        setYouboraConfigObject(adsObject, pluginConfig.getAsJsonObject("ads"), adsConfigFieldNames, adsBooleanConfigFieldNames);
        return youboraConfig;
    }

    public static Map<String, Object> setYouboraMediaDuration(JsonObject pluginConfig, double newMediaDuration) {
        mediaObject.put("duration", newMediaDuration);
        if (pluginConfig.has("media")){
            setYouboraConfigObject(mediaObject, pluginConfig.getAsJsonObject("media"), mediaConfigFieldNames, mediaBooleanConfigFieldNames);
        }
        return youboraConfig;
    }
    private static void setYouboraConfig(JsonObject pluginConfig, PKMediaConfig mediaConfig, Player player){
        log.d( "YouboraConfig setYouboraConfig");

        youboraConfig = defaultYouboraConfig;
        if (mediaConfig != null) {
            PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

            String playResource = source.getUrl();
            Uri url = Uri.parse(source.getUrl());
            if (url.getPath().contains("/playManifest/")) {
                Uri alt = url.buildUpon()
                        .appendQueryParameter("clientTag", CLIENT_TAG)
                        .appendQueryParameter("playSessionId", player.getSessionId().toString())
                        .build();
                playResource = alt.toString();
            }
            log.d("Youbora playResource = " + playResource);
            mediaObject.put("resource", playResource);
            Long duration = mediaConfig.getMediaEntry().getDuration() / 1000;
            log.d("Youbora update duration = " + duration.doubleValue());

            mediaObject.put("duration", duration.intValue()); //Duration should be sent in secs
        }
        if (pluginConfig != null) {

            //set these values on the root object
            setYouboraConfigObject(youboraConfigObject, pluginConfig, youboraConfigFieldNames, youboraBooleanConfigFieldNames);

            if (pluginConfig.has("media")){
                setYouboraConfigObject(mediaObject, pluginConfig.getAsJsonObject("media"), mediaConfigFieldNames, mediaBooleanConfigFieldNames);
            }
            if (pluginConfig.has("ads")){
                setYouboraConfigObject(adsObject, pluginConfig.getAsJsonObject("ads"), adsConfigFieldNames, adsBooleanConfigFieldNames);
            }
            if (pluginConfig.has("properties")){
                setYouboraConfigObject(propertiesObject, pluginConfig.getAsJsonObject("properties"), propertiesConfigFieldNames, null);
            }
            if (pluginConfig.has("extraParams")){
                setYouboraConfigObject(extraParamsObject, pluginConfig.getAsJsonObject("extraParams"), extraConfigFieldNames, null);
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
