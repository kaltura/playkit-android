package com.kaltura.playkit.addon.cast;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itanbarpeled on 13/12/2016.
 */


abstract class CastConfigHelper {



    JSONObject getCustomData(CastInfo castInfo) {


        String uiConf = castInfo.getUiConfId();
        String fileFormat = castInfo.getFormat();
        String entryId = castInfo.getMediaEntryId();
        String partnerId = castInfo.getPartnerId();
        String adTagUrl = castInfo.getAdTagUrl();
        String sessionInfo = getSessionInfo(castInfo);
        String mwEmbedUrl = castInfo.getMwEmbedUrl();


        JSONObject customData = new JSONObject();
        JSONObject embedConfig = new JSONObject();


        setEmbedConfig(customData, embedConfig, uiConf, fileFormat, entryId, partnerId, adTagUrl, sessionInfo, mwEmbedUrl);


        return customData;
    }




    private void setEmbedConfig(JSONObject customData, JSONObject embedConfig, String uiConf,
                                        String fileFormat, String entryId,
                                        String partnerId, String adTagUrl, String sessionInfo,
                                        String mwEmbedUrl) {

        try {

            embedConfig.put("lib", mwEmbedUrl);
            embedConfig.put("publisherID", partnerId);
            embedConfig.put("uiconfID", uiConf);
            embedConfig.put("entryID", entryId);

            setFlashVars(embedConfig, sessionInfo, adTagUrl, fileFormat, entryId);

            customData.put("embedConfig", embedConfig);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    protected abstract String getSessionInfo(CastInfo castInfo);


    private void setFlashVars(JSONObject embedConfig, String sessionInfo, String adTagUrl,
                                           String fileFormat, String entryId) {

        try {

            JSONObject flashVars = getFlashVars(sessionInfo, adTagUrl, fileFormat, entryId);

            if (flashVars.length() > 0) {

                embedConfig.put("flashVars", flashVars);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private JSONObject getFlashVars(String sessionInfo, String adTagUrl,
                                           String fileFormat, String entryId) {

        JSONObject flashVars = new JSONObject();

        setProxyData(flashVars, sessionInfo, fileFormat, entryId);
        setDoubleClickPlugin(flashVars, adTagUrl);

        return flashVars;
    }




    protected abstract void setProxyData(JSONObject flashVars, String sessionData,
                                         String fileFormat, String entryId);



    private void setDoubleClickPlugin(JSONObject flashVars, String adTagUrl) {

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
