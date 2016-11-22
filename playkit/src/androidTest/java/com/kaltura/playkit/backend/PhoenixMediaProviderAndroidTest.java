package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.backend.phoenix.data.AssetInfo;
import com.kaltura.playkit.backend.phoenix.data.AssetResult;
import com.kaltura.playkit.backend.phoenix.data.ResultAdapter;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.ParamsRequestElement;
import com.kaltura.playkit.connect.RequestElement;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.connect.SessionProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class PhoenixMediaProviderAndroidTest extends BaseTest {

    public static final String BaseUrl = "http://52.210.223.65:8080/v4_1/api_v3/";//"http://52.210.223.65:8080/v4_0/api_v3/";
    public static final String KS = "djJ8MTk4fAZXObQaPfvkEqBWfZkZfbruAO1V3CYGwE4OdvqojvsjaNMeN8yYtqgCvtpFiKblOayM9Xq5d2wHFCBAkbf7ju9-H4CrWrxOg7qhIRQUzqPz";
    public static final String MediaId = "258656";//frozen
    public static final String MediaId4 = "258655";//shrek
    public static final String MediaId2 = "437800";//vild
    public static final String MediaId3 = "259295";//the salt of earth
    public static final String MediaId5 = "258574";//gladiator  HD id- 508408  SD id- 397243
    public static final String Format = "Mobile_Devices_Main_HD";
    public static final String Format2 = "Mobile_Devices_Main_SD";
    public static String FrozenAssetInfo = "mock/phoenix.asset.get.258656.json";
    public static final int PartnerId = 198;


    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return BaseUrl;
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

    SessionProvider AnonymSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return BaseUrl;
        }

        @Override
        public String getKs() {
            return null;
        }

        @Override
        public int partnerId() {
            return PartnerId;
        }
    };


    RequestQueue testExecutor;

    PhoenixMediaProvider phoenixMediaProvider;

    public PhoenixMediaProviderAndroidTest() {
    }

    @Before
    public void setUp() {
        testExecutor = new Executor();


    }

    @Test
    public void testResponseParsing() {

        phoenixMediaProvider = new PhoenixMediaProvider().setSessionProvider(ksSessionProvider).setAssetId(MediaId).setReferenceType("media").setFormats(Format).setRequestExecutor(testExecutor);
        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                // resume();
                assertTrue(response.isSuccess());
                assertTrue(response.getResponse() != null);
                assertTrue(response.getResponse().getId().equals(MediaId));
                assertTrue(response.getResponse().getSources().size() == 1);
                assertTrue(response.getResponse().getDuration() == 2237);
                //PhoenixMediaProviderAndroidTest.this.wait(1);

                phoenixMediaProvider.setAssetId(MediaId5).setFormats(Format, Format2).setRequestExecutor(APIOkRequestsExecutor.getSingleton()).load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(MediaId5));
                            assertTrue(response.getResponse().getSources().size() == 2);
                            //assertTrue(response.getResponse().getDuration() == 2237);

                        } else {
                            assertNotNull(response.getError());
                            Log.e("PhoenixMediaProvider", "asset can't be played: "+response.getError().getMessage());
                        }


                        //phoenixMediaProvider.setAssetId(MediaId2).setFormat(Format2)
                        PhoenixMediaProviderAndroidTest.this.resume();

                    }
                });
            }
        });

        wait(1);

    }


    @Test
    public void testPreFetchedAsset() {
        PKMediaEntry mediaEntry = null;
        AssetInfo assetInfo = null;
        final JsonReader jsonReader;
        try {
            jsonReader = new JsonReader(new InputStreamReader(
                    InstrumentationRegistry.getTargetContext().getAssets().open(FrozenAssetInfo)));

            AssetResult assetResult = new GsonBuilder().registerTypeAdapter(AssetResult.class, new ResultAdapter()).create().fromJson(jsonReader, AssetResult.class);
            assetInfo = assetResult.asset;
            assertNotNull(assetInfo);
            mediaEntry = PhoenixMediaProvider.getMediaEntry(assetInfo, Arrays.asList(Format, Format2));

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(mediaEntry);
        assertEquals(2, mediaEntry.getSources().size());

    }


    @Test
    public void testAnonymousFetch() {
        new PhoenixMediaProvider().setSessionProvider(AnonymSessionProvider).setAssetId(MediaId).setReferenceType("media").setFormats(Format).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                assertTrue(response.isSuccess());
                assertNotNull(response.getResponse());
                assertTrue(response.getResponse() instanceof PKMediaEntry);
            }
        });
    }

    @Test
    public void testErrorHandling() {
        /* TODO:
        * invalid ks
        * invalid assetid
        * wrong server url
        * invalid response structure
        * check server error object handling*/


    }


    /**
     * mock executor that reads precreated files that includes the mediaAsset/get response as if retrieved
     * from the server.
     * the mock response file name is constructed from the request and parameters.
     * [phoenix.serviceName.actionName.assetId.json]
     *
     */
    class Executor implements RequestQueue {

        @Override
        public String queue(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public String queue(ParamsRequestElement action) {
            return null;
        }

        @Override
        public ResponseElement execute(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public void cancelAction(String actionId) {

        }

        @Override
        public void clearActions() {

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
                    String action = actionIdx == -1 ? "" : url.substring(actionIdx + ACTION.length());

                    if (request.getBody() != null) {
                        JsonParser parser = new JsonParser();
                        JsonElement body = parser.parse(request.getBody());
                        //parsing from response -> String assetId = body.getAsJsonObject().getAsJsonObject("result").getAsJsonPrimitive("id").getAsString();
                        String identifier ="";

                        if(body.getAsJsonObject().has("id")) {
                            identifier = body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

                        } else if (service.equals("multirequest")) {
                            if(body.getAsJsonObject().getAsJsonObject("1").getAsJsonPrimitive("service").getAsString().equals("licensedUrl")){
                                identifier = "licensedUrl";
                            }
                        }

                        if(identifier.equals("")){
                            request.onComplete((ResponseElement) Accessories.<String>buildResult(null, ErrorElement.MediaNotFound.message("mock file can't be traced from data")));
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
