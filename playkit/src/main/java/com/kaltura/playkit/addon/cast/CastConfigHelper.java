package com.kaltura.playkit.addon.cast;

import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itanbarpeled on 13/12/2016.
 */


class CastConfigHelper {


    // TODO itan - remove all this temps from here
    private static final String TEMP_LIB_URL_OVP = "https://kgit.html5video.org/pulls/3156/";
    private static final String TEMP_LIB_URL_LOCAL = "http://192.168.162.231/html5.kaltura/mwEmbed/";
    private static final String TEMP_LIB_URL_OTT = "http://player-227562931.eu-west-1.elb.amazonaws.com/v2.51.receiver.rc2/mwEmbed/";
    private static final String TEMP_AD_TAG_URL = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=xml_vmap1&unviewed_position_start=1&cust_params=sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator=";



    static JSONObject getCustomData(KCastInfo kCastInfo) {


        String uiConf = kCastInfo.getUiConfId();
        String fileFormat = kCastInfo.getFormat();
        String entryId = kCastInfo.getMediaEntryId();
        int partnerId = kCastInfo.getPartnerId();
        String adTagUrl = kCastInfo.getAdTagUrl();
        String ks = kCastInfo.getKs();
        String mwEmbedUrl = kCastInfo.getMwEmbedUrl();
        String initObject = kCastInfo.getInitObject();


        JSONObject customData = new JSONObject();

        try {

            JSONObject embedConfig = getEmbedConfig(uiConf, fileFormat, entryId, partnerId, adTagUrl, ks, mwEmbedUrl, initObject);
            customData.put("embedConfig", embedConfig);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return customData;
    }




    private static JSONObject getEmbedConfig(String uiConf, String fileFormat, String entryId,
                                             int partnerId, String adTagUrl, String ks,
                                             String mwEmbedUrl, String initObject) {

        JSONObject embedConfig = new JSONObject();

        try {

            embedConfig.put("lib", mwEmbedUrl);
            embedConfig.put("publisherID", partnerId);
            embedConfig.put("uiconfID", uiConf);
            embedConfig.put("entryID", entryId);

            setFlashVars(embedConfig, ks, adTagUrl, fileFormat, entryId, initObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return embedConfig;

    }


    private static void setFlashVars(JSONObject embedConfig, String ks, String adTagUrl,
                                           String fileFormat, String entryId,
                                           String initObject) {

        try {

            JSONObject flashVars = getFlashVars(ks, adTagUrl, fileFormat, entryId, initObject);

            if (flashVars.length() > 0) {

                embedConfig.put("flashVars", flashVars);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private static JSONObject getFlashVars(String ks, String adTagUrl,
                                           String fileFormat, String entryId,
                                           String initObject) {

        JSONObject flashVars = new JSONObject();

        setProxyData(flashVars, ks, fileFormat, entryId, initObject);
        setDoubleClickPlugin(flashVars, adTagUrl);

        return flashVars;
    }




    private static void setProxyData(JSONObject flashVars, String ks, String fileFormat,
                                     String entryId, String initObject) {

        // TODO itan - check that ks and initObject aren't both not null

        JSONObject proxyData = new JSONObject();
        JSONObject configObject = new JSONObject();

        if (!TextUtils.isEmpty(ks)) {

            try {

                configObject.put("flavorassets", new JSONObject().put("filters", new JSONObject().put("include", new JSONObject().put("Format", new JSONArray().put(fileFormat)))));
                configObject.put("baseentry", new JSONObject().put("vars", new JSONObject().put("isTrailer", "" + false)));
                proxyData.put("config", configObject);

                //proxyData.put("initObj", initObject);
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


    // TODO itan - remove this mock method
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


}
