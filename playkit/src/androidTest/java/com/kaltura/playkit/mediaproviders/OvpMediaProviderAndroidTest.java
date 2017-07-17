package com.kaltura.playkit.mediaproviders;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestConfiguration;
import com.kaltura.netkit.connect.request.RequestElement;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.Accessories;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.RestrictionError;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.api.ovp.KalturaOvpParser;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.kaltura.playkit.mediaproviders.MockParams.DRMEntryIdAnm;
import static com.kaltura.playkit.mediaproviders.MockParams.DRMEntryIdAnmDuration;
import static com.kaltura.playkit.mediaproviders.MockParams.DRMEntryIdUsr;
import static com.kaltura.playkit.mediaproviders.MockParams.DRMEntryIdUsrDuration;
import static com.kaltura.playkit.mediaproviders.MockParams.MockEmptyEntryId;
import static com.kaltura.playkit.mediaproviders.MockParams.MockMsgsEntryId;
import static com.kaltura.playkit.mediaproviders.MockParams.NonDRMEntryId;
import static com.kaltura.playkit.mediaproviders.MockParams.NonDRMEntryIdDuration;
import static com.kaltura.playkit.mediaproviders.MockParams.NotFoundEntryId;
import static com.kaltura.playkit.mediaproviders.MockParams.OvpBaseUrl;
import static com.kaltura.playkit.mediaproviders.MockParams.OvpPartnerId;
import static com.kaltura.playkit.mediaproviders.MockParams.OvpUserKS;
import static com.kaltura.playkit.mediaproviders.MockParams.RestrictedEntryId;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class OvpMediaProviderAndroidTest extends BaseTest {

    private RequestQueue testExecutor;
    private KalturaOvpMediaProvider kalturaOvpMediaProvider;


    private SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return OvpBaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(OvpUserKS));
            }
        }

        @Override
        public int partnerId() {
            return OvpPartnerId;
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
    public void testEntryInfoWithDrmFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        loadMediaByEntryId(DRMEntryIdUsr, DRMEntryIdUsrDuration, 7, ksSessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
            @Override
            public void execute(ResultElement<PKMediaEntry> data) {
                PKMediaSource firstSource = data.getResponse().getSources().get(0);
                assertNotNull(firstSource.getDrmData());
                assertTrue(firstSource.getDrmData().size() >= 0);
                assertTrue(firstSource.getUrl().endsWith("mpd"));
                assertTrue(firstSource.getMediaFormat().equals(PKMediaFormat.dash));

                PKMediaSource secondSource = data.getResponse().getSources().get(1);
                assertTrue(secondSource.getDrmData().size() >= 0);
                assertTrue(secondSource.getUrl().endsWith("mpd"));
                assertTrue(secondSource.getMediaFormat().equals(PKMediaFormat.dash));
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testAnonymousSessionEntryInfoWithDrmFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        loadMediaByEntryId(DRMEntryIdAnm, DRMEntryIdAnmDuration, 1, ksSessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
            @Override
            public void execute(ResultElement<PKMediaEntry> data) throws AssertionError {
                PKMediaSource firstSource = data.getResponse().getSources().get(0);
                assertNotNull(firstSource.getDrmData());
                assertTrue(firstSource.getDrmData().size() == 2);
                assertTrue(firstSource.getUrl().endsWith("mpd"));
                assertTrue(firstSource.getMediaFormat().equals(PKMediaFormat.dash));

                /*someone added drm data to the third retrieved source (applehttp), so this section is not valid
                PKMediaSource secondSource = data.getResponse().getSources().get(1);
                assertTrue(secondSource.getDrmData().size() == 0);
                assertTrue(secondSource.getUrl().endsWith("m3u8"));
                assertTrue(secondSource.getMediaFormat().equals(PKMediaFormat.hls_clear));*/
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }


    @Test
    public void testEmptyResponseRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(MockEmptyEntryId).setRequestExecutor(testExecutor);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.d(TAG, "response " + response);

                try {
                    assertNotNull(response);
                    assertNotNull(response.getError());
                    PKLog.w(TAG, "Content can't be played:\n" + response.getError());

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

    public void testCancelRequest() {

        final AtomicReference<AssertionError> failure = new AtomicReference<>();
        final KalturaOvpMediaProvider mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(NonDRMEntryId);

                    mediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            failure.set(new AssertionError("cancel didn't work"));
                            fail("should have been canceled");
                        }
                    });
                    OvpMediaProviderAndroidTest.this.resume();

        wait(1);
        try {
            TimeUnit.MILLISECONDS.sleep(615); // can be used to check cancel in different points of execution
            // mostly 625 milliseconds and up resulted in request finished execution, before the cancel was activated.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mediaProvider.cancel();
        try {
            TimeUnit.SECONDS.sleep(8); // to make sure all callbacks finished - if executed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(failure.get() == null);
    }




    @Test
    public void testEntryInfoSuccessFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        loadMediaByEntryId(NonDRMEntryId, NonDRMEntryIdDuration, 7, ksSessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
            @Override
            public void execute(ResultElement<PKMediaEntry> data) {
                PKMediaSource firstSource = data.getResponse().getSources().get(0);
                assertTrue(firstSource.getDrmData().size() >= 0);
                assertTrue(firstSource.getUrl().endsWith("mpd"));

                PKMediaSource secondSource = data.getResponse().getSources().get(1);
                assertTrue(secondSource.getDrmData().size() >= 0);
                assertTrue(secondSource.getUrl().endsWith("mpd"));
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testLiveEntryInfoFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        final String entryId = "0_cb9k71rb";
        new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(entryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        PKLog.d(TAG, "got PKMediaEntry object: checking content");
                        assertTrue(response.getResponse() != null);
                        assertTrue(response.getResponse().getId().equals(entryId));
                        assertTrue(response.getResponse().getSources().size() == 2);

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

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }


    private void loadMediaByEntryId(final String entryId, final int expectedDuration, final int expectedSrcsCount, SessionProvider sessionProvider, final AtomicReference<AssertionError> failure, final TestBlock<ResultElement<PKMediaEntry>> testBlock) {
        new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(entryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        PKLog.d(TAG, "got PKMediaEntry object: checking content");
                        assertTrue(response.getResponse() != null);
                        assertTrue(response.getResponse().getId().equals(entryId));
                        //assertTrue(response.getResponse().getSources().size() == expectedSrcsCount);
                        assertTrue(response.getResponse().getDuration() == expectedDuration);
                        PKLog.i(TAG, "PKMediaEntry validated successfully");

                    } else {
                        assertNotNull(response.getError());
                        PKLog.d(TAG, "got error on PKMediaEntry loading:" + response.getError());
                        fail("failed on entry loading:" + response.getError());
                    }

                    if (testBlock != null) {
                        testBlock.execute(response);
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

    @Test
    public void textLoadMediaWithEmptyKs() {
        SessionProvider sessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return MockParams.OvpBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(""));
                }
            }

            @Override
            public int partnerId() {
                return MockParams.OvpPartnerId;
            }
        };

        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(MockParams.NonDRMEntryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        PKLog.d(TAG, "got PKMediaEntry object: checking content");
                        assertTrue(response.getResponse() != null);
                        assertTrue(response.getResponse().getId().equals(MockParams.NonDRMEntryId));
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

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    /**
     * expected - failure since entry should not be found
     */
    @Test
    public void testEntryInfoFailedFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(NotFoundEntryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    assertNotNull(response);
                    assertNotNull(response.getError());
                    assertTrue(response.getError().getCode().equals("INVALID_ENTRY_ID") || response.getError().getCode().equals("ENTRY_ID_NOT_FOUND"));

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed on entry validation:" + e.getMessage());
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
    public void testEntryInfoWithMessagesFetch() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(RestrictedEntryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    assertNotNull(response);
                    assertNotNull(response.getError());
                    assertNull(response.getResponse());
                    assertTrue(response.getError() instanceof RestrictionError);
                    assertTrue(((RestrictionError) response.getError()).getExtra().equals(RestrictionError.Restriction.NotAllowed));
                    PKLog.i(TAG, "Anonymous user got restriction error: " + response.getError());

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("response should have return an error");
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
    public void testQALoadRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(MockMsgsEntryId).setRequestExecutor(testExecutor);
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
                        assertTrue(response.getResponse().getId().equals(MockMsgsEntryId));
                        assertTrue(response.getResponse().getSources().size() == 2);
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
        public void setDefaultConfiguration(RequestConfiguration config) {

        }

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

        @Override
        public void enableLogs(boolean enable) {

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

                        //String assetId = NotFoundEntryId;//body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

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
