package com.kaltura.magikapp.magikapp.menu;

import android.text.TextUtils;

import java.util.List;

/**
 * Created by zivilan on 01/01/2017.
 */

public class MenuHelper {
    private static MenuHelper sMenuHelper = null;
    private List<MenuItem> mMenuItems;

    private MenuHelper() {
    }

    public static MenuHelper getInstance() {
        if (sMenuHelper == null) {
            sMenuHelper = new MenuHelper();
        }
        return sMenuHelper;
    }

    public List<MenuItem> getMenuItems(){
        return mMenuItems;
    }

    public static int getPositionByMenuType(String menuType) {
        if(!TextUtils.isEmpty(menuType)) {
            for (int i = 0; i < getInstance().mMenuItems.size(); i++) {
                if (getInstance().mMenuItems.get(i).getStringType().equals(menuType)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getPositionByMenuId(int menuId) {
        for (int i = 0; i < mMenuItems.size(); i++) {
            if (mMenuItems.get(i).getId() == menuId) {
                return i;
            }
        }

        return -1;
    }

    public static class MenuType {
        public static final String HOME = "Home";
        public static final String EPG = "EPG";
        public static final String LIVE_TV = "LiveTV";
        public static final String MOVIES = "Movies";
        public static final String CATEGORY = "Category";
        public static final String CATEGORY_2_3 = "Category_2_3"; // TODO change typo
        public static final String CATEGORY_16_9 = "Category_16_9";
        public static final String MY_TV = "My TV";
        public static final String MYTV = "MyTV";
        public static final String SEPARATOR = "MenuSeperator"; //TODO change typo
        public static final String SETTINGS = "Settings";
        public static final String SEARCH = "Search";
        public static final String PURCHASES = "OnlinePurchases";
        public static final String LOGIN = "StartLogin";
        public static final String SUBSCRIPTIONS = "Subscriptions";
        public static final String EXTERNAL_WEB = "External web";
        public static final String COMPANION = "Companion";
        public static final String SERIES = "Series";
        public static final String COVER = "Cover";
        public static final String SOCIAL_TV = "SocialTV";
        public static final String SWITCH_PROFILE = "SwitchProfile";
        public static final String LOG_OUT = "Logout";
        public static final String OFFONLINE = "OffOnLine";

        public static final String SETTINGS_ACCOUNT = "Account";
        public static final String SETTINGS_PRIVACY = "Privacy";
        public static final String SETTINGS_NOTIFICATIONS = "Notofications";
        public static final String SETTINGS_DOWNLOADS = "Downloads";
        public static final String SETTINGS_ABOUT_US = "AboutUs";
        public static final String SETTINGS_CONTACT_US = "ContactUs";
        public static final String SETTINGS_TERMS_OF_USE = "TermsOfUse";
        public static final String SETTINGS_DEVELOPER_OPTIONS = "DevOptions";

    }
}
