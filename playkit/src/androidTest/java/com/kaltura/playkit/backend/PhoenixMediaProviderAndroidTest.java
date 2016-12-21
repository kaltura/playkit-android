package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.APIDefines;
import com.kaltura.playkit.backend.phoenix.OttSessionProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaAsset;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
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
import java.util.Arrays;
import java.util.List;

import static com.kaltura.playkit.backend.MockParams.ChannelId;
import static com.kaltura.playkit.backend.MockParams.FormatHD;
import static com.kaltura.playkit.backend.MockParams.FormatSD;
import static com.kaltura.playkit.backend.MockParams.FrozenAssetInfo;
import static com.kaltura.playkit.backend.MockParams.MediaId;
import static com.kaltura.playkit.backend.MockParams.MediaId2;
import static com.kaltura.playkit.backend.MockParams.MediaId5;
import static com.kaltura.playkit.backend.MockParams.PnxBaseUrl;
import static com.kaltura.playkit.backend.MockParams.PnxKS;
import static com.kaltura.playkit.backend.MockParams.PnxPartnerId;
import static com.kaltura.playkit.backend.MockParams.PnxPassword;
import static com.kaltura.playkit.backend.MockParams.PnxUsername;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class PhoenixMediaProviderAndroidTest extends BaseTest {

    OttSessionProvider testSession = new OttSessionProvider(PnxBaseUrl, PnxPartnerId);

    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return PnxBaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(PnxKS));
            }
        }

        @Override
        public int partnerId() {
            return PnxPartnerId;
        }
    };

    SessionProvider InvalidSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return PnxBaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(null);
            }
        }

        @Override
        public int partnerId() {
            return PnxPartnerId;
        }
    };

    RequestQueue testExecutor;

    PhoenixMediaProvider phoenixMediaProvider;

    public PhoenixMediaProviderAndroidTest() {
        super("PhoenixMediaProviderAndroidTest");
    }

    @Before
    public void setUp() {
        testExecutor = new Executor();
    }

    @Test
    public void testResponseParsing() {

        phoenixMediaProvider = new PhoenixMediaProvider().setSessionProvider(ksSessionProvider).
                setAssetId(MediaId).setMediaType(APIDefines.MediaType.Vod).setFormats(FormatHD).setRequestExecutor(testExecutor);
        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {

                assertTrue(response.isSuccess());
                assertTrue(response.getResponse() != null);
                assertTrue(response.getResponse().getId().equals(MediaId));
                assertTrue(response.getResponse().getSources().size() == 1);
                assertTrue(response.getResponse().getDuration() == 2237);
                assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Vod));

                phoenixMediaProvider.setAssetId(MediaId5).setFormats(FormatHD, FormatSD).setRequestExecutor(APIOkRequestsExecutor.getSingleton()).load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(MediaId5));
                            assertTrue(response.getResponse().getSources().size() == 2);
                            assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Vod));

                        } else {
                            assertNotNull(response.getError());
                            Log.e("PhoenixMediaProvider", "asset can't be played: " + response.getError().getMessage());
                        }

                        PhoenixMediaProviderAndroidTest.this.resume();

                    }
                });
            }
        });

        wait(1);

    }

    @Test
    public void textRemoteLoading() {
        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setMediaType(APIDefines.MediaType.Vod).setAssetId(MediaId5)
                .setFormats(FormatHD, FormatSD);

        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(MediaId5));
                    assertTrue(response.getResponse().getSources().size() == 2);
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Vod));

                } else {
                    assertNotNull(response.getError());
                    fail("asset can't be played: " + response.getError().getMessage());
                }

                PhoenixMediaProviderAndroidTest.this.resume();

            }
        });
        wait(1);
    }

    @Test
    public void textLiveRemoteLoading() {

        final OttSessionProvider ottSessionProvider = new OttSessionProvider(PnxBaseUrl, PnxPartnerId);
        ottSessionProvider.startSession(PnxUsername, PnxPassword, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {

                if (response.error != null) {
                    fail(response.error.getMessage());
                    resume();

                } else {
                    phoenixMediaProvider = new PhoenixMediaProvider()
                            .setSessionProvider(ottSessionProvider)
                            .setMediaType(APIDefines.MediaType.Channel).setAssetId(ChannelId)
                            .setFormats(FormatHD, FormatSD);

                    phoenixMediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            if (response.isSuccess()) {
                                assertTrue(response.getResponse() != null);
                                assertTrue(response.getResponse().getId().equals(ChannelId));
                                assertTrue(response.getResponse().getSources().size() == 2);
                                assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Live));

                            } else {
                                assertNotNull(response.getError());
                                Log.e("PhoenixMediaProvider", "asset can't be played: " + response.getError());
                            }

                            PhoenixMediaProviderAndroidTest.this.resume();

                        }
                    });
                }
            }
        });

        wait(1);
    }

    @Test
    public void testLoadCancel() {

        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(testSession)
                .setMediaType(APIDefines.MediaType.Vod).setAssetId(MediaId5)
                .setFormats(FormatHD, FormatSD);

        testSession.startSession(PnxUsername, PnxPassword, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error != null) {
                    fail("failed to start session: " + response.error.getMessage());
                    resume();
                    resume();
                } else {
                    PKLog.i("phoenix testing", "session ready start testing");

                    loadCancelTest1();

                    while (testWaitCount.getCount() > 1) {
                    }

                    loadCancelTest2(true);
                }
            }
        });
        wait(2);
    }

    private void loadCancelTest1() {
        PKLog.i("phoenix testing", "starting load 1:");

        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.e("phoenix testing", "load completion on a canceled load");
                fail("this request should have been canceled");
                resume();
            }
        });

        PKLog.d("phoenix testing", "cancel load 1");
        phoenixMediaProvider.cancel();

        PKLog.d("phoenix testing", "starting load 2:");
        phoenixMediaProvider.setAssetId(MediaId).setFormats(FormatSD).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(MediaId));
                    assertTrue(response.getResponse().getSources().size() == 1);
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Vod));

                    PKLog.d("phoenix testing", "starting load 3");
                    phoenixMediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            fail("this request has been canceled");
                            resume();
                        }
                    });
                    PKLog.d("phoenix testing", "cancel load 3?");
                    phoenixMediaProvider.cancel();

                    resume(1000);

                } else {
                    assertNotNull(response.getError());
                    Log.e("PhoenixMediaProvider", "MediaEntry loading failed: " + response.getError().getMessage());
                    resume();
                }

            }
        });
    }

    private void loadCancelTest2(final boolean cancelAtEnd) {
        PKLog.i("phoenix testing", "starting load 1:");
        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.e("phoenix testing", "load completion on a canceled load 1");
                fail("load 1 should have been canceled");
                resume();
            }
        });
        PKLog.i("phoenix testing", "starting load 2:");
        phoenixMediaProvider.setAssetId(MediaId5).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.e("phoenix testing", "load completion on a canceled load 2");
                fail("load 2 should have been canceled");
                resume();
            }
        });
        PKLog.i("phoenix testing", "starting load 3:");
        phoenixMediaProvider.setAssetId(MediaId2).setFormats(FormatHD, FormatSD).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (cancelAtEnd) {
                    PKLog.e("phoenix testing", "load completion on a canceled load 3");
                    fail("load 3 should have been canceled");
                    resume();
                } else {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(MediaId2));
                    assertTrue(response.getResponse().getSources().size() == 2);
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Vod));
                    resume();
                }
            }
        });

        if (cancelAtEnd) {
            phoenixMediaProvider.cancel();

            resume();
        }
    }


    @Test
    public void testPreFetchedAsset() {
        PKMediaEntry mediaEntry = null;
        KalturaMediaAsset assetInfo = null;
        final JsonReader jsonReader;
        try {
            jsonReader = new JsonReader(new InputStreamReader(
                    InstrumentationRegistry.getTargetContext().getAssets().open(FrozenAssetInfo)));

            BaseResult asset = PhoenixParser.parse(jsonReader);

            assertNotNull(asset);
            mediaEntry = PhoenixMediaProvider.getMediaEntry(assetInfo, Arrays.asList(FormatHD, FormatSD), APIDefines.AssetReferenceType.Media);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(mediaEntry);
        assertEquals(2, mediaEntry.getSources().size());

    }


    @Test
    public void testInvalidSession() {
        new PhoenixMediaProvider().setSessionProvider(InvalidSessionProvider).setAssetId(MediaId).setMediaType(APIDefines.MediaType.Vod).setFormats(FormatHD).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                assertFalse(response.isSuccess());
                assertNotNull(response.getError());
            }
        });
    }

    @Test
    public void testMultiresponseParsing() {
        String multiresponseWithError = "{\n" +
                "  \"executionTime\": 0.1118046,\n" +
                "  \"result\": [\n" +
                "    {\n" +
                "      \"objectType\": \"KalturaLoginSession\",\n" +
                "      \"refreshToken\": \"03e6b38cee2041baa089fabe3f86fe1d\",\n" +
                "      \"ks\": \"djJ8MTk4fJk--dJo2deSWHQ4Dtb60UlyCE86jgz_Y38N0CV6j0yzMjtolKkSaOjfyol_asfuP1-Fxdmmv_qHPtNJbraA3tZiahqeCI9ddec9p5pFB2pz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"error\": {\n" +
                "        \"objectType\": \"KalturaAPIException\",\n" +
                "        \"message\": \"KS expired\",\n" +
                "        \"code\": \"500016\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String multiresponseSuccess = "{\n" +
                "  \"executionTime\": 0.1415303,\n" +
                "  \"result\": [\n" +
                "    {\n" +
                "      \"objectType\": \"KalturaLoginSession\",\n" +
                "      \"refreshToken\": \"fe7de6d35f7a421181632e3ec64d5a8f\",\n" +
                "      \"ks\": \"djJ8MTk4fITddhHq4H7GYSJ78X7wR-A7z6NdHtjn-RUdDUmkG7xcLY-iu4WJmGAWGo2O9n9_YvVU9Q4sdsAs6Ste2TffDGvaZiNSY3SOjBduBH_U-_eA\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"objectType\": \"KalturaLicensedUrl\",\n" +
                "      \"altUrl\": \"\",\n" +
                "      \"mainUrl\": \"http://62.42.236.193:5555/shss/LIVE$CUP001/2.ism/Manifest?start=LIVE&end=END&device=HSS_PC_CLR_RB_HD\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Object parsed = PhoenixParser.parse(multiresponseSuccess);
        assertTrue(parsed instanceof List);
        assertTrue(((List) parsed).size() == 2);

        parsed = PhoenixParser.parse(multiresponseWithError);
        assertTrue(parsed instanceof List);
        assertTrue(((List) parsed).size() == 2);
        assertTrue(((List) parsed).get(1) instanceof BaseResult);
        assertTrue(((BaseResult) (((List) parsed).get(1))).error != null);
    }

    @Test
    public void testPrimitiveResponseParsing() {
        String response = "{\n" +
                "  \"executionTime\": 0.2519926,\n" +
                "  \"result\": true\n" +
                "}";

        String sameRequestError = "{\n" +
                "  \"executionTime\": 0.0004225,\n" +
                "  \"result\": {\n" +
                "    \"error\": {\n" +
                "      \"objectType\": \"KalturaAPIException\",\n" +
                "      \"message\": \"Invalid KS format\",\n" +
                "      \"code\": \"500015\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Object parsed = PhoenixParser.parse(response);
        assertTrue(parsed instanceof String);
        assertTrue(parsed.equals("true"));

        parsed = PhoenixParser.parse(sameRequestError);
        assertTrue(parsed instanceof BaseResult);
        assertTrue(((BaseResult) parsed).error != null);

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
                        JsonElement body = parser.parse(request.getBody());
                        //parsing from response -> String assetId = body.getAsJsonObject().getAsJsonObject("result").getAsJsonPrimitive("id").getAsString();
                        String identifier = "";

                        if (body.getAsJsonObject().has("id")) {
                            identifier = body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

                        } else if (service.equals("multirequest")) {
                            if (body.getAsJsonObject().getAsJsonObject("1").getAsJsonPrimitive("service").getAsString().equals("licensedUrl")) {
                                identifier = "licensedUrl";
                            }
                        }

                        if (identifier.equals("")) {
                            request.onComplete((ResponseElement) Accessories.<String>buildResult(null, ErrorElement.NotFound.message("mock file can't be traced from data")));
                            return;
                        }
                        //assertNotNull(assetId);
                        String inputFile = "mock/phoenix." + service + "." + action + "." + identifier + ".json";

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
