package com.kaltura.playkit.plugins.configs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class TVPAPIInitObject {

    public static final String SITE_GUID   = "SiteGuid";
    public static final String API_USER    = "ApiUser";
    public static final String DOMAIN_ID   = "DomainID";
    public static final String UDID_KEY    = "UDID";
    public static final String API_PASS    = "ApiPass";
    public static final String PLATFORM    = "Platform";
    public static final String LOCALE      = "Locale";

    private int SiteGuid;
    private String ApiUser;
    private int DomainId;
    private String UDID;
    private String ApiPass;
    private String Platform;
    private TVPAPILocale Locale;

    public TVPAPIInitObject(int siteGuid, String apiUser, int domainId, String UDID, String apiPass, String platform, TVPAPILocale locale) {
        SiteGuid = siteGuid;
        ApiUser = apiUser;
        DomainId = domainId;
        this.UDID = UDID;
        ApiPass = apiPass;
        Platform = platform;
        Locale = locale;
    }

    public int getSiteGuid() {
        return SiteGuid;
    }

    public void setSiteGuid(int siteGuid) {
        SiteGuid = siteGuid;
    }

    public String getApiUser() {
        return ApiUser;
    }

    public void setApiUser(String apiUser) {
        ApiUser = apiUser;
    }

    public int getDomainId() {
        return DomainId;
    }

    public void setDomainId(int domainId) {
        DomainId = domainId;
    }

    public String getUDID() {
        return UDID;
    }

    public void setUDID(String UDID) {
        this.UDID = UDID;
    }

    public String getApiPass() {
        return ApiPass;
    }

    public void setApiPass(String apiPass) {
        ApiPass = apiPass;
    }

    public String getPlatform() {
        return Platform;
    }

    public void setPlatform(String platform) {
        Platform = platform;
    }

    public TVPAPILocale getLocale() {
        return Locale;
    }

    public void setLocale(TVPAPILocale locale) {
        Locale = locale;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SITE_GUID, SiteGuid);
        jsonObject.addProperty(API_USER, ApiUser);
        jsonObject.addProperty(DOMAIN_ID, DomainId);
        jsonObject.addProperty(UDID_KEY, UDID);
        jsonObject.addProperty(API_PASS, ApiPass);
        jsonObject.addProperty(PLATFORM, Platform);


        JsonElement element = new Gson().toJsonTree(Locale);
        JsonObject object = element.getAsJsonObject();
        jsonObject.add(LOCALE, object);
        return jsonObject;
    }
}
