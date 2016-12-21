package com.kaltura.playkit.backend;

/**
 * Created by tehilarozin on 21/12/2016.
 */

public class MockParams {
    //OVP:
    public static final String OvpBaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage

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
    public static final String OvpAnonymousKS = "djJ8MjIyMjQwMXzXI4NeVu8er1kyU5oUr9CQfR79mb3mpSxSnRM99MaITqbLMQMmATdEhAyESU7-IW7YxYwDdHvd2XPz7xVVCaV1y1fIx34NM69w9pJLcrZPiw==";


    //OTT:
    public static final String PnxBaseUrl = "http://api-preprod.ott.kaltura.com/v4_1/api_v3/";//"http://52.210.223.65:8080/v4_1/api_v3/";

    public static final String PnxUsername = "albert@gmail.com";
    public static final String PnxPassword = "123456";
    public static final int PnxPartnerId = 198;

    public static final String PnxKS = "djJ8MTk4fAZXObQaPfvkEqBWfZkZfbruAO1V3CYGwE4OdvqojvsjaNMeN8yYtqgCvtpFiKblOayM9Xq5d2wHFCBAkbf7ju9-H4CrWrxOg7qhIRQUzqPz";

    public static final String MediaId = "258656";//frozen
    public static final String MediaId4 = "258655";//shrek
    public static final String MediaId2 = "437800";//vild
    public static final String MediaId3 = "259295";//the salt of earth
    public static final String MediaId5 = "258574";//gladiator  HD id- 508408  SD id- 397243

    public static final String FormatHD = "Mobile_Devices_Main_HD";
    public static final String FormatSD = "Mobile_Devices_Main_SD";

    public static String FrozenAssetInfo = "mock/phoenix.asset.get.258656.json";

}
