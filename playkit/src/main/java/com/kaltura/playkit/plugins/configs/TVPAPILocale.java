package com.kaltura.playkit.plugins.configs;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class TVPAPILocale {
    public static final String LOACALE_USER_STATE = "LocaleUserState";
    public static final String LOACALE_COUNTRY    = "LocaleCountry";
    public static final String LOACALE_DEVICE     = "LocaleDevice";
    public static final String LOACALE_LANGUAGE   = "LocaleLanguage";


    // "Locale":
    private String LoacaleUserState;
    private String LocaleCountry;
    private String LocaleDevice;
    private String LocaleLanguage;

    public TVPAPILocale(String loacaleUserState, String localeCountry, String localeDevice, String localeLanguage) {
        LoacaleUserState = loacaleUserState;
        LocaleCountry = localeCountry;
        LocaleDevice = localeDevice;
        LocaleLanguage = localeLanguage;
    }

    public String getLoacaleUserState() {
        return LoacaleUserState;
    }

    public void setLoacaleUserState(String loacaleUserState) {
        LoacaleUserState = loacaleUserState;
    }

    public String getLocaleCountry() {
        return LocaleCountry;
    }

    public void setLocaleCountry(String localeCountry) {
        LocaleCountry = localeCountry;
    }

    public String getLocaleDevice() {
        return LocaleDevice;
    }

    public void setLocaleDevice(String localeDevice) {
        LocaleDevice = localeDevice;
    }

    public String getLocaleLanguage() {
        return LocaleLanguage;
    }

    public void setLocaleLanguage(String localeLanguage) {
        LocaleLanguage = localeLanguage;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(LOACALE_USER_STATE, LoacaleUserState);
        jsonObject.addProperty(LOACALE_COUNTRY, LocaleCountry);
        jsonObject.addProperty(LOACALE_DEVICE, LocaleDevice);
        jsonObject.addProperty(LOACALE_LANGUAGE, LocaleLanguage);
        return jsonObject;
    }
}
