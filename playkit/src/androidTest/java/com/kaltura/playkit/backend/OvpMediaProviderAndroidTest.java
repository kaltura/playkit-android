package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.KalturaOvpParser;
import com.kaltura.playkit.backend.ovp.OvpSessionProvider;
import com.kaltura.playkit.backend.ovp.data.PrimitiveResult;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.RequestElement;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

import static com.kaltura.playkit.backend.SessionProviderAndroidTest.OvpLoginId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class OvpMediaProviderAndroidTest extends BaseTest {

    public static final String BaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    public static final String KS = "djJ8MjIwOTU5MXwcaDHViEJi-52CzAnb3BKDJBCwaKIntAFqGF3PlOHEWr421m_WSc9gdQi6QFBUM3C1aJIvD5kIzG-MA8mLkhLthG-3LNjx4iBQZAE4Am-71ySqC33bqF5FQbKJEqrAikJoxmrOE_uvPbwSa-nVfhtf";
    public static final String EntryId1 = "1_1h1vsv3z";
    public static final String EntryId2 = "1_ztdp5s5d";
    public static final String EntryId3 = "0_tb83i9pr"; //should get error - not found
    public static final String EntryIdEmpty = "0_5huwy2pz"; //should get error - empty content
    public static final int PartnerId = 2209591;
    public static final String LoginId = "tehila.rozin@kaltura.com";
    public static final String Password = "abcd1234*";


    public static final String QABaseUrl = "http://qa-apache-testing-ubu-01.dev.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    public static final String QAKS = "ZTU0NWY5NTM4NWM2OTg3YzY0YjJkMDU1Y2M4NjgwNjc0MDQ4YmQzOXwyNjIxOzI2MjE7MTQ4MTgxMDI3NzsyOzE0ODE3MjM4NzcuMzYzODthbGxhX2xpZGljaEB5YWhvby5jb207KixkaXNhYmxlZW50aXRsZW1lbnQ7Ow==";
    public static final String QAEntryId = "0_q4nkfriz";
    public static final int QAPartnerId = 2621;
    public static final int EntryId1Duration = 102000;
    public static final int EntryId2Duration = 100000;

    RequestQueue testExecutor;
    KalturaOvpMediaProvider kalturaOvpMediaProvider;


    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return BaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(KS));
            }
        }

        @Override
        public int partnerId() {
            return PartnerId;
        }
    };

    SessionProvider qaSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return QABaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(QAKS));
            }
        }

        @Override
        public int partnerId() {
            return QAPartnerId;
        }
    };


    public OvpMediaProviderAndroidTest() {
        super("OvpMediaProviderAndroidTest");
    }

    @Before
    public void setUp() {
        testExecutor = new Executor();
    }


    @Test
    public void testEntryInfo1SuccessFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    loadMediaByEntryId(EntryId1, EntryId1Duration, 8, sessionProvider, failure);

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEntryInfo2SuccessFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    loadMediaByEntryId(EntryId2, EntryId2Duration, 8, sessionProvider, failure);

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private void loadMediaByEntryId(final String entryId, final int expectedDuration, final int expectedSrcsCount, OvpSessionProvider sessionProvider, final AtomicReference<AssertionError> failure) {
        new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(entryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        PKLog.d(TAG, "got PKMediaEntry object: checking content");
                        assertTrue(response.getResponse() != null);
                        assertTrue(response.getResponse().getId().equals(entryId));
                        assertTrue(response.getResponse().getSources().size() == expectedSrcsCount);
                        assertTrue(response.getResponse().getDuration() == expectedDuration);
                        PKLog.i(TAG, "PKMediaEntry validated successfully");

                    } else {
                        assertNotNull(response.getError());
                        PKLog.d(TAG, "got error on PKMediaEntry loading:" + response.getError());
                        fail("failed on entry loading:" + response.getError());
                    }

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed on entry validation:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
    }

    /**
     * expected - failure since entry should not be found
     */
    @Test
    public void testEntryInfoFailedFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {

                    new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(EntryId3).load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            try {
                                assertNotNull(response);
                                assertNotNull(response.getError());
                                assertTrue(response.getError().getCode().equals("INVALID_ENTRY_ID")||response.getError().getCode().equals("ENTRY_ID_NOT_FOUND"));

                            } catch (AssertionError e) {
                                failure.set(e);
                                fail("failed on entry validation:" + e.getMessage());
                            } finally {
                                OvpMediaProviderAndroidTest.this.resume();
                            }
                        }
                    });

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testQALoadRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(qaSessionProvider).setEntryId(QAEntryId).setRequestExecutor(testExecutor);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.d(TAG, "response " + response);

                try {
                    assertNotNull(response);
                    if (!response.isSuccess()) {
                        assertNotNull(response.getError());
                        PKLog.w(TAG, "Content can't be played:\n" + response.getError());

                    } else {
                        assertNotNull(response.getResponse());
                        assertTrue(response.getResponse().getId().equals(QAEntryId));
                        assertTrue(response.getResponse().getSources().size() == 1); // format "hdnetworkmanifest" is excluded
                        assertTrue(response.getResponse().getDuration() == 136000);
                    }

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed to assert response:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEmptyResponseRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(EntryIdEmpty).setRequestExecutor(testExecutor);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.d(TAG, "response " + response);

                try {
                    assertNotNull(response);
                    //if (!response.isSuccess()) {
                        assertNotNull(response.getError());
                        PKLog.w(TAG, "Content can't be played:\n" + response.getError());

                    /*} else {
                        assertNotNull(response.getResponse());
                        assertTrue(response.getResponse().getId().equals(QAEntryId));
                        assertTrue(response.getResponse().getSources().size() == 1); // format "hdnetworkmanifest" is excluded
                        assertTrue(response.getResponse().getDuration() == 136000);
                    }*/

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed to assert response:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testQAMachineMediaFromSources() {

        KalturaOvpMediaProvider mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(qaSessionProvider).setEntryId(QAEntryId);
        mediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response != null && response.getError() == null) {
                    resume();
                } else {
                    fail("media creation faild");
                }
            }
        });

        wait(1);
    }

    @Test
    public void testMediaFromSources() {
        final OvpSessionProvider ovpSessionProvider = new OvpSessionProvider(BaseUrl);
        ovpSessionProvider.startSession(OvpLoginId, SessionProviderAndroidTest.OvpPassword, SessionProviderAndroidTest.OvpPartnerId,
                new OnCompletion<PrimitiveResult>() {
                    @Override
                    public void onComplete(PrimitiveResult response) {
                        if (response != null && response.error == null) {
                            KalturaOvpMediaProvider mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ovpSessionProvider).setEntryId(EntryId1);
                            mediaProvider.load(new OnMediaLoadCompletion() {
                                @Override
                                public void onComplete(ResultElement<PKMediaEntry> response) {
                                    if (response != null && response.getError() == null) {
                                        resume();
                                    } else {
                                        fail("media creation faild");
                                    }
                                }
                            });
                        } else {
                            fail("faild to start session");
                        }
                    }
                });
        wait(1);
    }

    @Test
    public void testPrimitiveResponseParsing() {
        String response = "true"; //2xUzNQszbmeucM9b_kAUUwT_0pvwxB4=
        //!! problem parsing "="charachter, causing malform json exception
        // -> we need to be able to parse it for the login request, we won't use parser mechanism in this case.
        String loginResponse = "djJ8MjIwOTU5MXzsuioBfhcT5p9oIFv3BN2fXlwzC9x1A1FEZCBS1gSG4e5eQYNwhMBm2t0Ooj4h0QeYjTpQZlCInCSkSk3dLf2sRmq0joAKH8Z32xUzNQszbmeucM9b_kAUUwT_0pvwxB4=";
        //String loginResponse = "{\"ks\":\"djJ8MjIwOTU5MXzsuioBfhcT5p9oIFv3BN2fXlwzC9x1A1FEZCBS1gSG4e5eQYNwhMBm2t0Ooj4h0QeYjTpQZlCInCSkSk3dLf2sRmq0joAKH8Z32xUzNQszbmeucM9b_kAUUwT_0pvwxB4=\"}";
        String loginRequestError = "{\n" +
                "  \"code\": \"USER_WRONG_PASSWORD\",\n" +
                "  \"message\": \"Wrong password supplied\",\n" +
                "  \"objectType\": \"KalturaAPIException\",\n" +
                "  \"args\": []\n" +
                "}";

        Object parsed = KalturaOvpParser.parse(response);
        assertTrue(parsed instanceof String);
        assertTrue(Boolean.TRUE.toString().equals(response));

        /*JsonReader reader = new JsonReader(new StringReader(loginResponse));
        reader.setLenient(true);
        parsed = KalturaOvpParser.parse(loginResponse);
        assertTrue(parsed instanceof String);
        assertTrue(((String)parsed).length() > 0);*/

        parsed = KalturaOvpParser.parse(loginRequestError);
        assertTrue(parsed instanceof BaseResult);
        assertTrue(((BaseResult) parsed).error != null);
        assertTrue(((BaseResult) parsed).error.getCode().equals("USER_WRONG_PASSWORD"));
    }

    /**
     * mock executor that reads precreated files that includes the mediaAsset/get response as if retrieved
     * from the server.
     * the mock response file name is constructed from the request and parameters.
     * [phoenix.serviceName.actionName.assetId.json]
     */
    class Executor implements RequestQueue {

        @Override
        public String queue(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public ResponseElement execute(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public void cancelRequest(String reqId) {

        }

        @Override
        public void clearRequests() {

        }

        @Override
        public boolean isEmpty() {
            return false;
        }


        class RequestHandler extends Thread {

            public static final String SERVICE = "/service/";
            public static final String ACTION = "/action/";
            private final RequestElement request;

            RequestHandler(RequestElement request) {
                this.request = request;
            }

            @Override
            public void run() {
                if (request != null) {
                    String url = request.getUrl();
                    int serviceIdx = url.indexOf(SERVICE);
                    int actionIdx = url.indexOf(ACTION);
                    String service = actionIdx == -1 ? url.substring(serviceIdx + SERVICE.length()) : url.substring(serviceIdx + SERVICE.length(), actionIdx);
                    String action = actionIdx == -1 ? "_" : url.substring(actionIdx + ACTION.length());

                    if (request.getBody() != null) {
                        JsonParser parser = new JsonParser();
                        JsonObject body = parser.parse(request.getBody()).getAsJsonObject();
                        String assetId = "";
                        if (body.has("2")) {
                            assetId = body.get("2").getAsJsonObject().getAsJsonPrimitive("entryId").getAsString();
                        }

                        //String assetId = EntryId3;//body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

                        String inputFile = "mock/ovp." + service + "." + action + "." + assetId + ".json";

                        try {
                            final JsonReader jsonReader = new JsonReader(new InputStreamReader(InstrumentationRegistry.getTargetContext().getAssets().open(inputFile)));


                            StringBuilder stringBuilder = new StringBuilder();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(InstrumentationRegistry.getTargetContext().getAssets().open(inputFile)));//new FileReader(inputFile));
                            try {
                                String line = bufferedReader.readLine();
                                while (line != null) {
                                    stringBuilder.append(line);
                                    line = bufferedReader.readLine();
                                }

                            } catch (IOException ex) {
                            } finally {
                                bufferedReader.close();
                            }

                            request.onComplete(Accessories.buildResponse(stringBuilder.toString(), null));

                        } catch (IOException e) {
                            e.printStackTrace();
                            request.onComplete((ResponseElement) Accessories.<String>buildResult(null, ErrorElement.LoadError));
                        }


                    }

                }
            }
        }
    }
}
