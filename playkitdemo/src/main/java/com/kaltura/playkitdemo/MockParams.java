package com.kaltura.playkitdemo;

import com.kaltura.playkit.connect.SessionProvider;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 15/11/2016.
 */

public class MockParams {

    public static final String PhoenixBaseUrl = "http://52.210.223.65:8080/v4_0/api_v3/";
    public static final int PartnerId = 198;

    //result of login with : [username: albert@gmail.com, pass: 123456]
    public static final String KS = "djJ8MTk4fPjQkM96OQ7N4GBL73vbOrbUMx7QNMEdoJ5kc6pLFCbgoTHIOAAmIO3ny2Ro0MnKMqGEGpRGM2fq5schRQ8PzqODmb0yegckE6qH5j9hqNig";

    public static final String MediaId = "258656";//frozen
    public static final String MediaId2 = "437800";//vild
    public static final String MediaId3 = "259295";//the salt of earth

    public static final String MediaType = "media";

    public static final String Format = "Mobile_Devices_Main_HD";
    public static final String Format2 = "Mobile_Devices_Main_SD";
    public static String FrozenAssetInfo = "mock/phoenix.asset.get.258656.json";

    public static SessionProvider sessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return PhoenixBaseUrl;
        }

        @Override
        public String getKs() {
            return KS;
        }

        @Override
        public int partnerId() {
            return PartnerId;
        }
    };

    public static class UserFactory {

        static ArrayList<UserLogin> users;

        static {
            fillWithUsers();
        }

        public static UserLogin getUser() {
            int Min = 0;
            int Max = users.size() - 1;

            int index = Min + (int) (Math.random() * ((Max - Min) + 1));
            return users.get(index);
        }

        static void fillWithUsers() {
            users = new ArrayList<>();
            users.add(new UserLogin("albert@gmail.com", "123456"));
            users.add(new UserLogin("betsy@gmail.com", "123456"));
            users.add(new UserLogin("Alfred@gmail.com", "123456"));
            users.add(new UserLogin("ziv.ilan@kaltura.com", "123456"));
            users.add(new UserLogin("itan@b.com", "123456"));
        }

        public static class UserLogin {
            public String username;
            public String password;

            public UserLogin(String username, String password) {
                this.username = username;
                this.password = password;
            }
        }
    }
}
