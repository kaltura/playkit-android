package com.kaltura.magikapp.data;

/**
 * Created by zivilan on 02/01/2017.
 */

public class ConfigObjectConsts {

    public enum eIS_ENABLED{
        TRUE ("YES"),
        FALSE ("NO");

        String response;

        eIS_ENABLED(String res){
            response = res;
        }
    }

    public static final String STATUS = "status";
    public static final String VALID = "valid";
    public static final String KEY = "key";
    public static final String UDID = "udid";
    public static final String TOKEN = "token";
    public static final String CONFIG_PARAMS = "params";
    public static final String REFRESH_TOKEN_MARGIN = "refreshTokenSafetyMarginInSeconds";
    public static final String GID = "GID";
    public static final String CURRENCY_CODE = "CurrencyCode";
    public static final String INTERNAL_SETTINGS = "InternalSettings";
    public static final String BRAND_NAME = "brandName";
    public static final String PAGE_SIZE = "pageSize";
    public static final String BRAND_LINKS = "brandLinks";
    public static final String ABOUT_US = "aboutUs";
    public static final String JOIN = "join";
    public static final String EMAIL = "email";
    public static final String PROMOTON = "promotion";
    public static final String LOGO = "logo";
    public static final String MINIMAL_TIME_FOLLOW_ME = "minimalTimeForFollowMe";
    public static final String MNIMAL_PASSWORD_LENGH = "minimalPasswordLength";
    public static final String MAIN_MENU_ID = "MainMenuID";
    public static final String UI = "UI";
    public static final String COMPANION = "Companion";
    public static final String COMPANION_PARAMS = "Params";
    public static final String RECEIVER = "Receiver";
    public static final String APP_ID = "AppID";
    public static final String MEDIA_ID = "MediaID";
    public static final String SITE_GUID = "SiteGuid";
    public static final String ACCESS_TOKEN = "AccessToken";
    public static final String EPG_CHANNEL_ID = "EPGChannelID";
    public static final String PROGRAM_ID = "ProgramID";
    public static final String PLAY_MODE = "PlayMode";
    public static final String FILE_ID = "FileID";
    public static final String INIT_OBJECT = "InitObj";
    public static final String API_PASS = "ApiPass";
    public static final String API_USER = "ApiUser";
    public static final String LOCALE = "Locale";
    public static final String LOCALE_USER_STATE = "LocaleUserState";
    public static final String LOCAL_COUNTRY = "LocaleCountry";
    public static final String LOCAL_DEVICE = "LocaleDevice";
    public static final String LOCAL_LANGUAGE = "LocaleLanguage";
    public static final String GATEWAYS = "Gateways";
    public static final String JSON_GW = "JsonGW";
    public static final String FILE_FORMATS = "FilesFormat";
    public static final String MEDIA_TYPES = "MediaTypes";
    public static final String PERSONALIZATION_SERVER = "PersonalizationServer";
    public static final String LICENSE_SERVER = "LicenseServer";
    public static final String OPERATOR_ID = "operatorId";

    public static final String WIDEVINE = "Widevine";

    public static final String DEFAULT_IPNO = "IPNO";
    public static final String IS_ENABLED = "enabled";
    public static final String DEFAULT_IPNO_ID = "defaultId";
    public static final String RECEIVER_APPLICATION_ID = "applicationID";

    public static String COLOR = "colors";
    public static final String COLOR_PRIMARY = "primary";
    public static final String COLOR_SECONDARY = "secondary";
    public static final String COLOR_OTHER = "other";

    public static String COLOR_PRIMARY1 = "primary1";
    public static String COLOR_PRIMARY2 = "primary2";
    public static String COLOR_PRIMARY3 = "primary3";
    public static String COLOR_SECONDARY1 = "secondary1";
    public static String COLOR_SECONDARY2 = "secondary2";
    public static String COLOR_SECONDARY3 = "secondary3";
    public static String COLOR_SECONDARY4 = "secondary4";
    public static String COLOR_SECONDARY5 = "secondary5";
    public static String COLOR_SECONDARY6 = "secondary6";
    public static String COLOR_OTHER_DANGER = "danger";
    public static String COLOR_OTHER_SUCCESS = "success";
    public static String COLOR_OTHER_FACEBOOK = "facebook";
    public static String COLOR_OTHER_INFO = "info";


    public static final String IMAGE_RATIOS = "imageRatios";
    public static final String TYPES = "ratioByRatioIDMap";
    public static final String TESTTYPES = "test_types";
    public static final String DEFINITION_MEDIA_TYPES = "ratioTypeIDByMediaTypeMap";

    public static final String DETACHING = "detaching";

    public static final String VERSION = "version";
    public static final String APP_NAME ="appname" ;
    public static final String CLIENT_VERSION = "clientversion";
    public static final String IS_FORCE_UPDATE = "isforceupdate";

    public static final String PLATFORM = "platform";
    public static final String PLATFORM_InitObj = "Platform";

    public static final String TERMS_Of_SERVICE = "termsOfService";
    public static String DX_PERS_URL = "DxPers_URL";

    public static final String INAPP = "IAP";
    public static final String GOOGLE_PLAY = "googlePlay";
    public static final String PUBLIC_KEY = "publicKey";

    public static final String EPG = "EPG";
    public static final String CATCHUP = "catchUp";
    public static final String LIKE = "like";
    public static final String LIKE_LIVE = "likeLive";
    public static final String STARTOVER = "startOver";
    public static final String SEARCH = "search";
    public static final String SHOW_CHANNEL_NUMBER = "showChannelNumber";
    public static final String NUMBER_OF_RETRO_DAYS = "numberOfRetroDays";
    public static final String NUMBER_OF_WEEK_DAYS = "numberOfWeekDays";

    public static final String HAS_WATCHLIST = "hasWatchList";
    public static final String MULTI_USER = "supportMultiUser";
    public static final String RECORDINGS = "supportRecordings";
    public static final String PROXIMITY = "proximity";
    public static final String SHARING = "sharePrivacy";
    public static final String FACEBOOK_SHARING = "fbSharePrivacy";
    public static final String ANONYMOUS = "anonymousUserSupport";
    // public static final String CAN_PLAY_FREE_ITEMS = "anonymousUserSupport";
    public static final String CAN_PLAY_FREE_ITEMS = "canPlayFreeItems";

    public static final String SUSPENSION_BLOCK_ON_LOGIN = "suspensionBlockOnLogin";
    public static final String COMMERCIALIZATION = "hasCommercialization";
    public static final String LOGIN_WITH_FACEBOOK = "hasLoginWithFB";
    public static final String CREATE_NEW_ACCOUNT = "canCreateNewAccount";

    public static final String COMPANION_DETACHMENT = "companion";
    public static final String DEVICES = "devices";
    public static final String STB = "STB";
    public static final String CHROMECAST = "Chromecast";

    public static final String SWITCH_PROFILE = "switchProfile";
    public static final String SWITCH_PROFILE_SWITCH_USER = "canSwitchUser";
    public static final String SWITCH_PROFILE_ADD_NEW_USER = "canAddNewUser";
    public static final String SWITCH_PROFILE_PASSWORD_PROTECTED = "passwordProtected";

    public static final String DTG = "DTG";
    public static final String DTG_DOWNLOAD_CELLULAR_DATA = "enableDownloadOnCellularData";
    public static final String DTG_AUTO_RESUME_DOWNLOADS = "autoResumeDownloads";
    public static final String DTG_DOWNLOAD_QUOTA = "downloadQuota";

    public static final String ANALYTICS = "analytics";
    public static final String GOOGLE_ANALYTICS = "googleAnalytics";
    public static final String ANALYTICS_IDENTIFIER = "identifier";
    public static final String ANALYTICS_CRITISISM = "crittercism";
    public static final String ANALYTICS_FABRIC = "fabric";

    public static final String UX = "UX";
    public static final String UX_PAGES = "pages";
    public static final String UX_PAGES_SETTINGS = "settings";
    public static final String UX_PAGES_SETTINGS_SHOW_CONTACT_US = "showContactUs";
    public static final String UX_PAGS_SETTINGS_SHOW_BILLING_HISTORY = "showBillingHistory";
    public static final String UX_PAGES_LOGIN = "login";
    public static final String UX_PAGES_LOGIN_SHOW_VIDEO = "showVideo";
    public static final String UX_PAGES_LOGIN_SIGN_UP_FIELDS = "signupFields";
    public static final String UX_PAGES_LOGIN_SIGN_UP_FIELDS_LAST_NAME = "lastName";
    public static final String UX_PAGES_LOGIN_SIGN_UP_FIELDS_BIRTHDAY = "birthday";
    public static final String UX_PAGES_LOGIN_SIGN_UP_FIELDS_GENDER = "gender";
    public static final String UX_PAGES_MENU = "menu";
    public static final String UX_PAGES_MENU_SHOW_SETTINGS = "showSettings";
    public static final String UX_PAGES_MENU_SHOW_LOGIN = "showLogin";
    public static final String UX_PAGES_MENU_SHOW_FOOTER = "showFooter";
    public static final String UX_PAGES_HOME = "showTitles";
    public static final String UX_PAGES_HOME_SHOW_TITLES = "showTitles";
    public static final String UX_SHOW_SOCIAL_FEED = "showSocialFeed";
    public static final String UX_PAGES_SHOW_MYZONE_FULL_PAGE = "showMyZoneFullPage";

    public static final String SOCIAL = "social";
    public static final String SOCIAL_FACEBOOK = "facebook";
    public static final String SOCIAL_FACEBOOK_CAN_LOGIN = "canLogin";
    public static final String SOCIAL_FACEBOOK_CAN_SHARE_CONTENT = "canShareContent";
    public static final String SOCIAL_TWITTER = "twitter";
    public static final String SOCIAL_TWITTER_CAN_LOGIN = "canLogin";
    public static final String SOCIAL_TWITTER_CAN_SHARE_CONTENT = "canShareContent";


    public static final String KALTURA_PLAYER_DOMAIN_URL = "domainUrl";
    public static final String KALTURA_PLAYER_UI_CONF_ID = "uiConfId";
    public static final String KALTURA_PLAYER_PARTNER_ID = "partnerId";
    public static final String KALTURA_PLAYER = "KalturaPlayer";
    public static final String REMOTE_FILES = "RemoteFiles";
    public static final String REMOTE_FILES_SETTINGS = "Settings";


}
