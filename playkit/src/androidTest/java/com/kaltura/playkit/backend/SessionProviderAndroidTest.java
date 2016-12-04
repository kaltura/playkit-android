package com.kaltura.playkit.backend;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.BaseSessionProvider;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.OttSessionProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.ResultElement;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by tehilarozin on 28/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class SessionProviderAndroidTest extends BaseTest {

    public static String BaseUrl = "http://api-preprod.ott.kaltura.com/api_v3/"; //"http://52.210.223.65:8080/v4_1/api_v3/"
    public static final int PartnerId = 198;
    public static final String Username = "albert@gmail.com";
    public static final String Password = "123456";
    public static final String AssetId = "258574";
    public static final String Format = "Mobile_Devices_Main_HD";

    SessionProvider sessionProvider;

    @Test
    public void testOttSessionProvider(){
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(BaseUrl, PartnerId);
        ottSessionProvider.setSessionProviderListener(new BaseSessionProvider.SessionProviderListener() {
            @Override
            public void onError(ErrorElement error) {
                if(error == ErrorElement.SessionError){
                    Log.e("testOttSessionProvider", "failed to establish a session");
                    ottSessionProvider.startAnonymousSession(null);
                }
            }

            @Override
            public void ready() {
                ottSessionProvider.getKs(new OnCompletion<String>() {
                    @Override
                    public void onComplete(String response) {
                        Assert.assertNotNull(response);
                        Assert.assertFalse(response.equals(""));

                        new PhoenixMediaProvider()
                                .setSessionProvider(ottSessionProvider)
                                .setAssetId(AssetId)
                                .setReferenceType("media")
                                .setFormats(Format)
                                .load(new OnMediaLoadCompletion() {
                                    @Override
                                    public void onComplete(ResultElement<PKMediaEntry> response) {
                                        if(response != null && response.isSuccess()){
                                            Log.i("testOttSessionProvider", "we have mediaEntry");
                                            Assert.assertTrue(response.getResponse().getId().equals(AssetId));
                                            Assert.assertTrue(response.getResponse().getSources().size() == 1);
                                        }
                                        resume();
                                    }
                                });
                    }
                });

            }
        });
        ottSessionProvider.startSession(Username, Password, null);

        wait(1);
        //new OttSessionProvider("",0).setUsername("").setPasword().startSession();
        //new OttSessionProvider("",0).startSession("username", "password", "udid");
    }
}
