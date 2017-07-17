package com.kaltura.playkitdemo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tehilarozin on 15/11/2016.
 */

public class MockParams {

/*Ott Mock params: */
    public static final String PhoenixBaseUrl = "https://api-preprod.ott.kaltura.com/v4_2/api_v3/";
    public static final int OttPartnerId = 198;

    //result of login with : [username: albert@gmail.com, pass: 123456]
    public static final String PnxKS = "djJ8MTk4fH6bz_2197wFUNBqv2zRZ3h5YlTwiAEJZvVhJJut1pq13CMVSpgmD-NLVco4pJuthWU9b8Z_XEV7h6uvg5tpJbCj4ODWAzWhslokulcfUrgG0WISPD3wq1YWwD1lzuJ109OFrezv9Ih5Wa8qrUIsaz8=";

    public static final String SingMediaId = "480989";
    public static final String MediaId = "258656";//frozen
    public static final String MediaId2 = "437800";//vild-wV
    public static final String MediaId3 = "259295";//the salt of earth

    public static final String MediaType = "media";

    public static final String Format = "Mobile_Devices_Main_HD";
    public static final String Format_HD_Dash = "Mobile_Devices_Main_HD_Dash";
    public static final String Format_SD_Dash = "Mobile_Devices_Main_SD_Dash";
    public static final String Format2 = "Mobile_Devices_Main_SD";
    public static String FrozenAssetInfo = "mock/phoenix.asset.get.258656.json";
//---------------------------------------

/*Ovp Mock params: */
    public static final int OvpPartnerId = 2222401;
    public static final String OvpBaseUrl = "https://cdnapisec.kaltura.com/";
    public static final String NonDRMEntryId = "1_xay0wjby"; //works for user/anonymous
    public static final String DRMEntryIdUsr = "1_tmomdals"; //works for logged user
    public static final String DRMEntryIdAnm = "1_ytsd86sc"; //works for anonymous

    public static final String OvpUserKS = "djJ8MjIyMjQwMXx2RAtiYX9vr3hnwdyi1rM78jFD15pr8XYnbhu9iuUy3KXt_NEQK0JV9bdRdaBpohgY5mQW88kKvKu5EC15wfDceyj_37BTG3UYd5LvVa7GbmWxr9YrEpFTxrAPpgeMvYBV-mooSL7YbDqp_kvDqKW3";

    public enum UserType{Ott, Ovp}

    public static class UserFactory {

        static ArrayList<UserLogin> ottUsers;
        static ArrayList<UserLogin> ovpUsers;

        static {
            fillWithUsers();
        }

        public static UserLogin getUser(UserType type) {
            List<UserLogin> users = type == UserType.Ott ? ottUsers : ovpUsers;
            int Min = 0;
            int Max = users.size() - 1;

            int index = Min + (int) (Math.random() * ((Max - Min) + 1));
            return users.get(index);
        }

        static void fillWithUsers() {
            ottUsers = new ArrayList<>();
            ottUsers.add(new UserLogin("albert@gmail.com", "123456", 198));
            ottUsers.add(new UserLogin("betsy@gmail.com", "123456", 198));
            ottUsers.add(new UserLogin("Alfred@gmail.com", "123456", 198));
            ottUsers.add(new UserLogin("itan@b.com", "123456", 198));

            ovpUsers = new ArrayList<>();
            ovpUsers.add(new UserLogin("kaltura.fe@icloud.com", "abcd1234*", 2222401));
        }

        public static UserLogin getDrmUser(UserType type) {
            switch (type){
                case Ovp:
                    return ovpUsers.get(0);

                default:
                    return null;
            }
        }

        public static class UserLogin {
            public String username;
            public String password;
            public int partnerId;

            public UserLogin(String username, String password, int partnerId) {
                this.username = username;
                this.password = password;
                this.partnerId = partnerId;
            }
        }
    }
}
