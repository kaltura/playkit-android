package com.kaltura.playkit.mediaproviders;

/**
 * Created by tehilarozin on 21/12/2016.
 */

public class MockParams {
    //OVP:
    //public static final String OvpBaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    public static final String OvpBaseUrl = "https://cdnapisec.kaltura.com/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    //public static final String OvpBaseUrl = "http://cdnapi.kaltura.com/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage

    // Demo account user
    public static final String NonDRMEntryIdAnm = "1_25q88snr"; //works for anonymous
    public static final int NonDRMEntryIdAnmDuration = 167000;

    public static final String NonDRMEntryId = "1_xay0wjby"; //works for user/anonymous
    public static final int NonDRMEntryIdDuration = 96000;

    public static final String DRMEntryIdAnm = "1_ytsd86sc"; //works for anonymous //1_3wzacuha
    public static final int DRMEntryIdAnmDuration = 102000;

    public static final String DRMEntryIdUsr = "1_tmomdals"; //works for logged user
    public static final int DRMEntryIdUsrDuration = 30094;

    //public static final String DRMEntryIdUsr2 = "1_i02uprfp"; //works for user

    public static final String RestrictedEntryId = "1_3wzacuha"; //restricted with drm not working with kaltura.fe
    public static final String NotFoundEntryId = "0_tb83i9pr"; //should get error - not found
    public static final String MockEmptyEntryId = "0_5huwy2pz"; //should get error - empty content
    public static final String MockMsgsEntryId = "0_q4nkfriz"; //should get error - has restriction

    public static final int OvpPartnerId = 2222401;
    public static final String OvpLoginId = "kaltura.fe@icloud.com";
    public static final String OvpPassword = "abcd1234*";
    public static final String OvpUserKS = "djJ8MjIyMjQwMXx2RAtiYX9vr3hnwdyi1rM78jFD15pr8XYnbhu9iuUy3KXt_NEQK0JV9bdRdaBpohgY5mQW88kKvKu5EC15wfDceyj_37BTG3UYd5LvVa7GbmWxr9YrEpFTxrAPpgeMvYBV-mooSL7YbDqp_kvDqKW3";
    public static final String OvpAnonymousKS = "djJ8MjIyMjQwMXzXI4NeVu8er1kyU5oUr9CQfR79mb3mpSxSnRM99MaITqbLMQMmATdEhAyESU7-IW7YxYwDdHvd2XPz7xVVCaV1y1fIx34NM69w9pJLcrZPiw==";

    //OTT:
    public static final String PnxBaseUrl = "https://api-preprod.ott.kaltura.com/v4_2/api_v3/";//"http://52.210.223.65:8080/v4_1/api_v3/";

    public static final String PnxUsername = "albert@gmail.com";
    public static final String PnxPassword = "123456";
    public static final int PnxPartnerId = 198;

    public static final String PnxKS = "djJ8MTk4fH6bz_2197wFUNBqv2zRZ3h5YlTwiAEJZvVhJJut1pq13CMVSpgmD-NLVco4pJuthWU9b8Z_XEV7h6uvg5tpJbCj4ODWAzWhslokulcfUrgG0WISPD3wq1YWwD1lzuJ109OFrezv9Ih5Wa8qrUIsaz8=";

    public static final String MediaId = "258656";//frozen //trailer 428345
    public static final String MediaId4 = "258655";//shrek
    public static final String ShlomoArMediaId = "485384";//vild
    public static final String SpongeMediaId = "258660";//sponge bob
    public static final String ToystoryMediaId = "485293";//toystory

    public static final String MediaId2_File_SD_Dash = "690398";//vild
    public static final String MediaId2_File_Main_SD = "690396";//vild
    public static final String MediaId2_File_Main_HD = "690395";//vild
    public static final String MediaId2_File_Web_HD = "690403";//vild

    public static final String Toystory_File_SD_Dash = "737631";//vild
    public static final String Toystory_File_Main_HD_Dash = "737630";//vild
    public static final String Toystory_File_Main_HD = "737629";//vild

    public static final String PnxNotEntitledMedia = "482731";
    public static final String PnxNoFilesFoundMedia = "482550";//no mediaFiles on asset


    public static final String MediaId3 = "259295";//the salt of earth
    public static final String MediaId5 = "258574";//gladiator  HD id- 508408  SD id- 397243
    public static final String ChannelId = "255854";

    public static final String FormatHD = "Mobile_Devices_Main_HD";
    public static final String FormatHDDash = "Mobile_Devices_Main_HD_Dash";
    public static final String FormatSD = "Mobile_Devices_Main_SD";
    public static final String WebHD = "Web HD";

    public static String FrozenAssetInfo = "mock/phoenix.asset.get.258656.json";


    public static final String OvpQaBaseUrl = "http://qa-apache-testing-ubu-01.dev.kaltura.com/";
}
