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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.format;

/**
 * Created by itanbarpeled on 07/12/2016.
 */
public class GoogleCastAddOn {


    // TODO itan - remove all this temps from here
    private static final String MOCK_DATA = "MOCK_DATA";
    private static final String TEMP_LIB_URL_OVP = "https://kgit.html5video.org/pulls/3156/";
    private static final String TEMP_LIB_URL_LOCAL = "http://192.168.162.231/html5.kaltura/mwEmbed/";
    private static final String TEMP_LIB_URL_OTT = "http://player-227562931.eu-west-1.elb.amazonaws.com/v2.51.receiver.rc2/mwEmbed/";
    private static final String TEMP_AD_TAG_URL = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=xml_vmap1&unviewed_position_start=1&cust_params=sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator=";



    // TODO itan - remove this temp method
    public static void getMediaInfoBuilder(OnCompletion<MediaInfo.Builder> completionListener) {



        JSONObject customData = new JSONObject();
        JSONObject embedConfig = new JSONObject();

        try {


            // OVP
            String entryId = "0_l1v5vzh3";
            embedConfig.put("lib", TEMP_LIB_URL_OVP);
            embedConfig.put("publisherID", "243342");
            embedConfig.put("uiconfID", "21099702");
            embedConfig.put("entryID", entryId);
            //embedConfig.put("flashVars", getFlashVars(null, TEMP_AD_TAG_URL, null, null));




            // OTT
            /*
            String entryId = "258656";
            embedConfig.put("lib", TEMP_LIB_URL_OTT);
            embedConfig.put("publisherID", "198");
            embedConfig.put("uiconfID", "8413355");
            embedConfig.put("entryID", entryId);
            embedConfig.put("flashVars", getFlashVars("temp_ks", TEMP_AD_TAG_URL, "Web SD", entryId));
            */



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



    private static JSONObject getMockInitObject() {

        JSONObject initObj = new JSONObject();
        JSONObject local = new JSONObject();

        try {

            local.put("LocaleDevice", "");
            local.put("LocaleLanguage", "en");
            local.put("LocaleCountry", "");
            local.put("LocaleUserState", "Unknown");
            initObj.put("Locale", local);

            initObj.put("UDID", "2345");
            initObj.put("ApiUser", "tvpapi_198");
            initObj.put("DomainID", "362595");
            initObj.put("ApiPass", "11111");
            initObj.put("SiteGuid", "739182");
            initObj.put("Platform", "Web");


        } catch (JSONException e) {
            e.printStackTrace();
        }


        return initObj;

    }


    public static void getMediaInfoBuilder(PlayerConfig playerConfig, SessionProvider sessionProvider,
                                           final String uiConf, final String fileFormat,
                                           final OnCompletion<MediaInfo.Builder> completionListener) {

        // initObj
        // base url

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

            embedConfig.put("lib", TEMP_LIB_URL_OTT);
            embedConfig.put("publisherID", partnerId);
            embedConfig.put("uiconfID", uiConf);
            embedConfig.put("entryID", entryId);
            embedConfig.put("flashVars", getFlashVars(ks, adTagUrl, fileFormat, entryId));
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



    private static JSONObject getFlashVars(String ks, String adTagUrl, String fileFormat, String entryId) {

        JSONObject flashVars = new JSONObject();

        setProxyData(flashVars, ks, fileFormat, entryId);
        setDoubleClickPlugin(flashVars, adTagUrl);

        return flashVars;
    }




    private static void setProxyData(JSONObject flashVars, String ks, String fileFormat, String entryId) {

        JSONObject proxyData = new JSONObject();
        JSONObject configObject = new JSONObject();

        if (!TextUtils.isEmpty(ks)) {

            try {


                configObject.put("flavorassets", new JSONObject().put("filters", new JSONObject().put("include", new JSONObject().put("Format", new JSONArray().put(fileFormat)))));
                configObject.put("baseentry", new JSONObject().put("vars", new JSONObject().put("isTrailer", "" + false)));
                proxyData.put("config", configObject);

                proxyData.put("initObj", getMockInitObject());
                //proxyData.put("ks", ks);

                proxyData.put("MediaID", entryId);
                proxyData.put("iMediaID", entryId);
                flashVars.put("proxyData", proxyData);

            } catch(JSONException e) {
                e.printStackTrace();
            }

        }
    }



    private static void setDoubleClickPlugin(JSONObject flashVars, String adTagUrl) {

        JSONObject doubleClick = new JSONObject();

        if (!TextUtils.isEmpty(adTagUrl)) {

            try {

                doubleClick.put("plugin", true);
                doubleClick.put("adTagUrl", adTagUrl);
                flashVars.put("doubleClick", doubleClick);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }




}
