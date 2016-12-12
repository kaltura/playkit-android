package com.kaltura.playkit.backend;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.OvpSessionProvider;
import com.kaltura.playkit.backend.ovp.data.PrimitiveResult;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.backend.phoenix.OttSessionProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.ads.interactivemedia.v3.impl.aa.c.error;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by tehilarozin on 28/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class SessionProviderAndroidTest extends BaseTest {

    public static String OttBaseUrl = "http://api-preprod.ott.kaltura.com/v4_1/api_v3/"; //"http://52.210.223.65:8080/v4_1/api_v3/";
    public static final int OttPartnerId = 198;
    public static final String OttUsername = "albert@gmail.com";
    public static final String OttPassword = "123456";
    public static final String AssetId = "258574";
    public static final String Format = "Mobile_Devices_Main_HD";

    public static String OvpBaseUrl = "http://www.kaltura.com/api_v3/";
    public static final int OvpPartnerId = 2209591;
    public static final String OvpLoginId = "tehila.rozin@kaltura.com";
    public static final String OvpPassword = "abcd1234*";
    public static final String EntryId = "1_1h1vsv3z";


    SessionProvider sessionProvider;

    @Test
    public void testOttSessionProvider() {
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(OttBaseUrl, OttPartnerId);

        ottSessionProvider.startSession(OttUsername, OttPassword, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error != null){
                    Log.e("testOttSessionProvider", "failed to establish a session: "+response.error.getMessage());
                } else{
                    ottSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            new PhoenixMediaProvider()
                                    .setSessionProvider(ottSessionProvider)
                                    .setAssetId(AssetId)
                                    .setReferenceType("media")
                                    .setFormats(Format)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOttSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(AssetId));
                                                assertTrue(response.getResponse().getSources().size() == 1);
                                            }
                                            resume();
                                        }
                                    });
                        }
                    });
                }
            }
        });

        wait(1);
    }

    @Test
    public void testOvpSessionProviderBaseFlow() {
        ovpSessionProvider = new OvpSessionProvider(OvpBaseUrl);

        ovpSessionProvider.startSession(OvpLoginId, OvpPassword, OvpPartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error != null){
                    Log.e("testOttSessionProvider", "failed to establish a session: "+response.error.getMessage());
                } else {
                    ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            new KalturaOvpMediaProvider()
                                    .setSessionProvider(ovpSessionProvider)
                                    .setEntryId(EntryId)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOvpSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(EntryId));
                                                // Assert.assertTrue(response.getResponse().getSources().size() == 1);
                                            }
                                            resume();
                                        }
                                    });
                        }
                    });
                }
            }
        });

        wait(1);
    }


    @Test
    public void testOvpAnonymousSession() {
        final OvpSessionProvider ovpSessionProvider = new OvpSessionProvider(OvpBaseUrl);

        ovpSessionProvider.startAnonymousSession(OvpPartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error != null){
                    Log.e("testAnonymousSession", "failed to establish anonymous session: "+response.error.getMessage());

                    if (response.error == ErrorElement.SessionError) {
                        fail("should have logged ");
                    }
                } else {
                    ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            Log.e("testAnonymousSession", "get ks = " + response.getResult());
                            new KalturaOvpMediaProvider()
                                    .setSessionProvider(ovpSessionProvider)
                                    .setEntryId(EntryId)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOvpSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(EntryId));
                                            }
                                            resume();
                                        }
                                    });
                        }
                    });
                }
            }
        });

        wait(1);
    }


    @Test
    public void testOvpSPError() {
        final OvpSessionProvider ovpSessionProvider = new OvpSessionProvider(OvpBaseUrl + "invalid/");
        ovpSessionProvider.startSession(OvpLoginId, OvpPassword, OvpPartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error != null){
                    assertNotNull(error);
                    assertTrue(response.error == ErrorElement.ConnectionError);
                } else {
                    fail("server url is invalid error sould have raised");
                }
            }
        });

        wait(1);
    }


    OvpSessionProvider ovpSessionProvider;

    @Test
    public void testOvpSP() {
        ovpSessionProvider = new OvpSessionProvider(OvpBaseUrl);

        testOvpSessionProviderBaseFlow();

        //wait(1);
        //while (testWaitCount.getCount() > 1){}

        testEndSession();

    }


    String testKs;

    private void testEndSession() {


        ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                assertNotNull(response.getResult());
                testKs = response.getResult();

                ovpSessionProvider.endSession(new OnCompletion<BaseResult>() {
                    @Override
                    public void onComplete(BaseResult response) {
                        if (response.error == null) {
                            APIOkRequestsExecutor.getSingleton().queue(BaseEntryService.list(ovpSessionProvider.baseUrl(),
                                    testKs, EntryId)
                                    .completion(new OnRequestCompletion() {
                                        @Override
                                        public void onComplete(ResponseElement response) {
                                            assertNotNull(response.getResponse());
                                            BaseResult parsedResponse = PhoenixParser.parse(response.getResponse());
                                            assertNotNull(parsedResponse);
                                            assertNotNull(parsedResponse.error);
                                            assertTrue(parsedResponse.error.getCode().toLowerCase().contains("invalid_ks"));

                                            new KalturaOvpMediaProvider()
                                                    .setSessionProvider(ovpSessionProvider)
                                                    .setEntryId(EntryId)
                                                    .load(new OnMediaLoadCompletion() {
                                                        @Override
                                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                                            //should renew session when using the ovpSessionProvider
                                                            assertTrue(response.getError() == null);
                                                            resume();
                                                        }
                                                    });
                                        }
                                    })
                                    .build());

                        } else {
                            PKLog.i(TAG, "got an error: " + response.error.getMessage());
                            //assertTrue(error.equals(fo));
                        }
                    }
                });
            }
        });
        wait(1);
    }

    //TODO add failure test, add test for expiration - check renewal of session
}
