package com.kaltura.playkit.plugins.youbora.pluginconfig;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.npaw.youbora.lib6.plugin.Options;

public class YouboraConfig {

    private String accountCode;

    private String username;

    private String userType;        // any string - free / paid etc.

    private String houseHoldId;    // which device is used to play

    private boolean obfuscateIP;   // ip in dahsbord will be encrytpted

    private boolean httpSecure; // youbora events will be sent via https

    private Media media;

    private Ads ads;

    private Properties properties;

    private ExtraParams extraParams;

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isObfuscateIP() {
        return obfuscateIP;
    }

    public boolean getHttpSecure() {
        return httpSecure;
    }

    public void setHttpSecure(boolean httpSecure) {
        this.httpSecure = httpSecure;
    }

    public String getHouseHoldId() {
        return houseHoldId;
    }

    public void setHouseHoldId(String houseHoldId) {
        this.houseHoldId = houseHoldId;
    }

    public void setObfuscateIP(boolean obfuscateIP) {
        this.obfuscateIP = obfuscateIP;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public Ads getAds() {
        return ads;
    }

    public void setAds(Ads ads) {
        this.ads = ads;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public ExtraParams getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(ExtraParams extraParams) {
        this.extraParams = extraParams;
    }


    public Options getYouboraOptions() {
        Options youboraLocalConfig =  new Options();

        youboraLocalConfig.setAccountCode(accountCode);
        youboraLocalConfig.setUsername(username);
        youboraLocalConfig.setUserType(userType);
        youboraLocalConfig.setNetworkObfuscateIp(obfuscateIP);
        youboraLocalConfig.setHttpSecure(httpSecure);

        youboraLocalConfig.setParseHls(false);
        youboraLocalConfig.setParseCdnNode(false);


        youboraLocalConfig.setDeviceCode(null); //TODO  // List of device codes http://mapi.youbora.com:8081/devices
        youboraLocalConfig.setContentCdn(null);



        youboraLocalConfig.setContentIsLive(media.getIsLive());
        youboraLocalConfig.setContentIsLiveNoSeek((media.getIsDVR() != null) ? !media.getIsDVR() : null);
        youboraLocalConfig.setContentDuration(media.getDuration());
        youboraLocalConfig.setContentTitle(media.getTitle());
        youboraLocalConfig.setContentTitle2(media.getTitle2());
        youboraLocalConfig.setContentTransactionCode(media.getTransactionCode());


        youboraLocalConfig.setAdResource(null);
        youboraLocalConfig.setAdCampaign(ads.getCampaign());
        youboraLocalConfig.setAdTitle("");



        youboraLocalConfig.setContentMetadata(getPropertiesBundle());


        if (ads.getExtraParams() != null) {
            youboraLocalConfig.setAdExtraparam1(ads.getExtraParams().getParam1());
            youboraLocalConfig.setAdExtraparam2(ads.getExtraParams().getParam2());
            youboraLocalConfig.setAdExtraparam3(ads.getExtraParams().getParam3());
            youboraLocalConfig.setAdExtraparam4(ads.getExtraParams().getParam4());
            youboraLocalConfig.setAdExtraparam5(ads.getExtraParams().getParam5());
            youboraLocalConfig.setAdExtraparam6(ads.getExtraParams().getParam6());
            youboraLocalConfig.setAdExtraparam7(ads.getExtraParams().getParam7());
            youboraLocalConfig.setAdExtraparam8(ads.getExtraParams().getParam8());
            youboraLocalConfig.setAdExtraparam9(ads.getExtraParams().getParam9());
            youboraLocalConfig.setAdExtraparam10(ads.getExtraParams().getParam10());
        }

        if (extraParams != null) {
            youboraLocalConfig.setExtraparam1(extraParams.getParam1());
            youboraLocalConfig.setExtraparam2(extraParams.getParam2());
            youboraLocalConfig.setExtraparam3(extraParams.getParam3());
            youboraLocalConfig.setExtraparam4(extraParams.getParam4());
            youboraLocalConfig.setExtraparam5(extraParams.getParam5());
            youboraLocalConfig.setExtraparam6(extraParams.getParam6());
            youboraLocalConfig.setExtraparam7(extraParams.getParam7());
            youboraLocalConfig.setExtraparam8(extraParams.getParam8());
            youboraLocalConfig.setExtraparam9(extraParams.getParam9());
            youboraLocalConfig.setExtraparam10(extraParams.getParam10());
        }
        return youboraLocalConfig;
    }


    private Bundle getPropertiesBundle() {
        if (getProperties() == null) {
            return new Bundle();
        }

        Properties prop = getProperties();
        Bundle propertiesBunble = new Bundle();
        propertiesBunble.putString("genre", (prop.getGenre() != null) ? prop.getGenre() : "");
        propertiesBunble.putString("type", (prop.getType() != null) ? prop.getType() : "");
        propertiesBunble.putString("transaction_type", (prop.getTransactionType() != null) ? prop.getTransactionType() : "");
        propertiesBunble.putString("year", (prop.getYear() != null) ? prop.getYear() : "");
        propertiesBunble.putString("cast", (prop.getCast() != null) ? prop.getCast() : "");
        propertiesBunble.putString("director", (prop.getDirector() != null) ? prop.getDirector() : "");
        propertiesBunble.putString("owner", (prop.getOwner() != null) ? prop.getOwner() : "");
        propertiesBunble.putString("parental", (prop.getParental() != null) ? prop.getParental() : "");
        propertiesBunble.putString("price", (prop.getPrice() != null) ? prop.getPrice() : "");
        propertiesBunble.putString("rating", (prop.getRating() != null) ? prop.getRating() : "");
        propertiesBunble.putString("audioType", (prop.getAudioType() != null) ? prop.getAudioType() : "");
        propertiesBunble.putString("audioChannels", (prop.getAudioChannels() != null) ? prop.getAudioChannels() : "");
        propertiesBunble.putString("device", (prop.getDevice() != null) ? prop.getDevice() : "");
        propertiesBunble.putString("quality", (prop.getQuality() != null) ? prop.getQuality() : "");
        return propertiesBunble;
    }

    public JsonObject toJson() {
        JsonPrimitive accountCode = new JsonPrimitive(getAccountCode() != null ? getAccountCode() : "");
        JsonPrimitive username = new JsonPrimitive(getUsername() != null ? getUsername() : "");
        JsonPrimitive userType = new JsonPrimitive(getUserType() != null ? getUserType() : "");
        JsonPrimitive houseHoldId = new JsonPrimitive(getHouseHoldId() != null ? getHouseHoldId() : "");
        JsonPrimitive isObfuscateIP = new JsonPrimitive(isObfuscateIP() ? true : false);
        JsonPrimitive httpSecure = new JsonPrimitive(getHttpSecure() ? true : false);

        JsonObject mediaEntry = getMediaJsonObject();
        JsonObject adsEntry = new JsonObject();
        adsEntry.addProperty("campaign", (getAds() != null && getAds().getCampaign() != null) ? getAds().getCampaign() : "");
        JsonObject propertiesEntry = getPropertiesJsonObject();
        JsonObject extraParamEntry = getExtraParamJsonObject();
        JsonObject youboraConfig = getYouboraConfigJsonObject(accountCode, username, userType, houseHoldId, isObfuscateIP, httpSecure, mediaEntry, adsEntry, propertiesEntry, extraParamEntry);
        return youboraConfig;
    }

    @NonNull
    private JsonObject getMediaJsonObject() {
        JsonObject mediaEntry = new JsonObject();
        if (getMedia() == null) {
            return mediaEntry;
        }

        Media media = getMedia();
        mediaEntry.addProperty("isLive", media.getIsLive() != null ? media.getIsLive() : Boolean.FALSE);
        mediaEntry.addProperty("title",  media.getTitle() != null ? media.getTitle() : "");
        if (media.getDuration() != null) {
            mediaEntry.addProperty("duration", media.getDuration());
        }
        return mediaEntry;
    }

    @NonNull
    private JsonObject getYouboraConfigJsonObject(JsonPrimitive accountCode, JsonPrimitive username, JsonPrimitive userType, JsonPrimitive houseHoldId, JsonPrimitive obfuscateIP, JsonPrimitive httpSecure,
                                                  JsonObject mediaEntry, JsonObject adsEntry, JsonObject propertiesEntry, JsonObject extraParamEntry) {
        JsonObject youboraConfig = new JsonObject();
        youboraConfig.add("accountCode", accountCode);
        youboraConfig.add("username", username);
        youboraConfig.add("userType", userType);
        youboraConfig.add("houseHoldId", houseHoldId);
        youboraConfig.add("obfuscateIP", obfuscateIP);
        youboraConfig.add("httpSecure", httpSecure);

        youboraConfig.add("media", mediaEntry);
        youboraConfig.add("ads", adsEntry);
        youboraConfig.add("properties", propertiesEntry);
        youboraConfig.add("extraParams", extraParamEntry);
        return youboraConfig;
    }

    @NonNull
    private JsonObject getPropertiesJsonObject() {
        JsonObject propertiesEntry = new JsonObject();
        if (getProperties() == null) {
            return propertiesEntry;
        }

        Properties prop = getProperties();
        propertiesEntry.addProperty("genre", (prop.getGenre() != null) ? prop.getGenre() : "");
        propertiesEntry.addProperty("type", (prop.getType() != null) ? prop.getType() : "");
        propertiesEntry.addProperty("transaction_type", (prop.getTransactionType() != null) ? prop.getTransactionType() : "");
        propertiesEntry.addProperty("year", (prop.getYear() != null) ? prop.getYear() : "");
        propertiesEntry.addProperty("cast", (prop.getCast() != null) ? prop.getCast() : "");
        propertiesEntry.addProperty("director", (prop.getDirector() != null) ? prop.getDirector() : "");
        propertiesEntry.addProperty("owner", (prop.getOwner() != null) ? prop.getOwner() : "");
        propertiesEntry.addProperty("parental", (prop.getParental() != null) ? prop.getParental() : "");
        propertiesEntry.addProperty("price", (prop.getPrice() != null) ? prop.getPrice() : "");
        propertiesEntry.addProperty("rating", (prop.getRating() != null) ? prop.getRating() : "");
        propertiesEntry.addProperty("audioType", (prop.getAudioType() != null) ? prop.getAudioType() : "");
        propertiesEntry.addProperty("audioChannels", (prop.getAudioChannels() != null) ? prop.getAudioChannels() : "");
        propertiesEntry.addProperty("device", (prop.getDevice() != null) ? prop.getDevice() : "");
        propertiesEntry.addProperty("quality", (prop.getQuality() != null) ? prop.getQuality() : "");
        return propertiesEntry;
    }

    @NonNull
    private JsonObject getExtraParamJsonObject() {
        JsonObject extraParamEntry = new JsonObject();
        if (getExtraParams() == null) {
            return extraParamEntry;
        }
        ExtraParams extraParams = getExtraParams();
        if (extraParams.getParam1() != null) {
            extraParamEntry.addProperty("param1", extraParams.getParam1());
        }
        if (extraParams.getParam2() != null) {
            extraParamEntry.addProperty("param2", extraParams.getParam2());
        }
        if (extraParams.getParam3() != null) {
            extraParamEntry.addProperty("param3", extraParams.getParam3());
        }
        if (extraParams.getParam4() != null) {
            extraParamEntry.addProperty("param4", extraParams.getParam4());
        }
        if (extraParams.getParam5() != null) {
            extraParamEntry.addProperty("param5", extraParams.getParam5());
        }
        if (extraParams.getParam6() != null) {
            extraParamEntry.addProperty("param6", extraParams.getParam6());
        }
        if (extraParams.getParam7() != null) {
            extraParamEntry.addProperty("param7", extraParams.getParam7());
        }
        if (extraParams.getParam8() != null) {
            extraParamEntry.addProperty("param8", extraParams.getParam8());
        }
        if (extraParams.getParam9() != null) {
            extraParamEntry.addProperty("param9", extraParams.getParam9());
        }
        if (extraParams.getParam10() != null) {
            extraParamEntry.addProperty("param10", extraParams.getParam10());
        }
        return extraParamEntry;
    }

    public void merge(YouboraConfig youboraConfigUiConf) {
        if (youboraConfigUiConf == null) {
            return;
        }

        if (TextUtils.isEmpty(accountCode)) {
            accountCode = youboraConfigUiConf.getAccountCode();
        }
        if (TextUtils.isEmpty(username)) {
            username =  youboraConfigUiConf.getUsername();
        }

        if (media != null) {
            if (youboraConfigUiConf.getMedia() != null) {
                if (media.getIsLive() == null) {
                    media.setIsLive(youboraConfigUiConf.getMedia().getIsLive());
                }
                if (media.getTitle() == null) {
                    media.setTitle(youboraConfigUiConf.getMedia().getTitle());
                }
                if (media.getDuration() == null) {
                    media.setDuration(youboraConfigUiConf.getMedia().getDuration());
                }
            }
        } else {
            media = youboraConfigUiConf.getMedia();
        }

        if (ads != null) {
            if (ads.getCampaign() == null) {
                if (youboraConfigUiConf.getAds() != null) {
                    ads.setCampaign(youboraConfigUiConf.getAds().getCampaign());
                }
            }
        } else {
            ads = youboraConfigUiConf.getAds();
        }

        if (properties != null) {
            if (youboraConfigUiConf.getProperties() != null) {
                Properties propUiConf = youboraConfigUiConf.getProperties();
                if (TextUtils.isEmpty((properties.getGenre()))) {
                    properties.setGenre(propUiConf.getGenre());
                }
                if (TextUtils.isEmpty((properties.getType()))) {
                    properties.setType(propUiConf.getType());
                }
                if (TextUtils.isEmpty((properties.getTransactionType()))) {
                    properties.setTransactionType(propUiConf.getTransactionType());
                }
                if (TextUtils.isEmpty((properties.getAudioChannels()))) {
                    properties.setAudioChannels(propUiConf.getAudioChannels());
                }
                if (TextUtils.isEmpty((properties.getAudioType()))) {
                    properties.setAudioType(propUiConf.getAudioType());
                }
                if (TextUtils.isEmpty((properties.getCast()))) {
                    properties.setCast(propUiConf.getCast());
                }
                if (TextUtils.isEmpty((properties.getDevice()))) {
                    properties.setDevice(propUiConf.getDevice());
                }
                if (TextUtils.isEmpty((properties.getDirector()))) {
                    properties.setDirector(propUiConf.getDirector());
                }
                if (TextUtils.isEmpty((properties.getOwner()))) {
                    properties.setOwner(propUiConf.getOwner());
                }
                if (TextUtils.isEmpty((properties.getParental()))) {
                    properties.setParental(propUiConf.getParental());
                }
                if (TextUtils.isEmpty((properties.getYear()))) {
                    properties.setYear(propUiConf.getYear());
                }
                if (TextUtils.isEmpty((properties.getQuality()))) {
                    properties.setQuality(propUiConf.getQuality());
                }
                if (TextUtils.isEmpty((properties.getRating()))) {
                    properties.setRating(propUiConf.getRating());
                }

            }
        } else {
            properties = youboraConfigUiConf.getProperties();
        }

        if (extraParams != null) {
            if (youboraConfigUiConf.getExtraParams() != null) {
                ExtraParams extraParamsUiConf = youboraConfigUiConf.getExtraParams();
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam1(extraParamsUiConf.getParam1());
                }
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam1(extraParamsUiConf.getParam1());
                }
                if (TextUtils.isEmpty((extraParams.getParam2()))) {
                    extraParams.setParam2(extraParamsUiConf.getParam2());
                }
                if (TextUtils.isEmpty((extraParams.getParam3()))) {
                    extraParams.setParam3(extraParamsUiConf.getParam3());
                }
                if (TextUtils.isEmpty((extraParams.getParam4()))) {
                    extraParams.setParam4(extraParamsUiConf.getParam4());
                }
                if (TextUtils.isEmpty((extraParams.getParam5()))) {
                    extraParams.setParam5(extraParamsUiConf.getParam5());
                }
                if (TextUtils.isEmpty((extraParams.getParam6()))) {
                    extraParams.setParam6(extraParamsUiConf.getParam6());
                }
                if (TextUtils.isEmpty((extraParams.getParam7()))) {
                    extraParams.setParam7(extraParamsUiConf.getParam7());
                }
                if (TextUtils.isEmpty((extraParams.getParam8()))) {
                    extraParams.setParam8(extraParamsUiConf.getParam8());
                }
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam9(extraParamsUiConf.getParam9());
                }
                if (TextUtils.isEmpty((extraParams.getParam10()))) {
                    extraParams.setParam10(extraParamsUiConf.getParam10());
                }
            }
        } else {
            extraParams = youboraConfigUiConf.getExtraParams();
        }

    }
}