package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.phoenix.data.AssetInfo;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class OvpMediaProviderAndroidTest extends BaseTest {

    public static final String BaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    public static final String KS = "djJ8MjIwOTU5MXzmCXI2UMvSAFJsHqm6rGgKFJ-pVSTVTY4ngtX-ER2VxIVBZxMK2d5MusI5Z_nIaLSos_sc-XiSBLL7yJM8xuhsrNmnzNTLp4CmfTgFErAVnWxd7h6rrXdkpqF4Wd0Nz2pu1YCK8FtaOoSNFB2yTe6Y";
    public static final String EntryId = "1_1h1vsv3z";
    public static final String EntryId2 = "1_ztdp5s5d";
    public static final int PartnerId = 2209591;
    public static String EntryInfo = "mock/ovp.multirequest..1_1h1vsv3z.json";


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

    KalturaOvpMediaProvider kalturaOvpMediaProvider;

    public OvpMediaProviderAndroidTest() {
    }

    @Before
    public void setUp() {
        testExecutor = new Executor();


    }

    @Test
    public void testResponseParsing() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider(ksSessionProvider, EntryId).setRequestExecutor(testExecutor);
        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                // resume();
                assertTrue(response.isSuccess());
                assertTrue(response.getResponse() != null);
                assertTrue(response.getResponse().getId().equals(EntryId));
                assertTrue(response.getResponse().getSources().size() == 6);
                assertTrue(response.getResponse().getDuration() == 102000);
                //kalturaOvpMediaProviderAndroidTest.this.wait(1);

                kalturaOvpMediaProvider.setRequestExecutor(APIOkRequestsExecutor.getSingleton()).load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(EntryId));
                            assertTrue(response.getResponse().getSources().size() == 6);
                            assertTrue(response.getResponse().getDuration() == 102000);

                        } else {
                            assertNotNull(response.getError());

                        }


                        //kalturaOvpMediaProvider.setAssetId(MediaId2).setFormat(Format2)
                        OvpMediaProviderAndroidTest.this.resume();

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
                    InstrumentationRegistry.getTargetContext().getAssets().open(EntryInfo)));
//KalturaOvpMediaProvider.KalturaOvpParser.parseMultiresponse(jsonReader.)
            /*AssetResult assetResult = new GsonBuilder().registerTypeAdapter(AssetResult.class, new OvpResultAdapter()).create().fromJson(jsonReader, AssetResult.class);
            assetInfo = assetResult.mediaAsset;
            assertNotNull(assetInfo);
            mediaEntry = kalturaOvpMediaProvider.getMediaEntry(assetInfo, Arrays.asList(Format, Format2));
*/
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(mediaEntry);
        assertEquals(2, mediaEntry.getSources().size());

    }


    /*@Test
    public void testAnonymousFetch() {
        new kalturaOvpMediaProvider(AnonymSessionProvider, MediaId, "media", Format).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                assertTrue(response.isSuccess());
                assertNotNull(response.getResponse());
                assertTrue(response.getResponse() instanceof PKMediaEntry);
            }
        });
    }*/

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
                        //JsonElement body = parser.parse(request.getBody());
                        //parsing from response -> String assetId = body.getAsJsonObject().getAsJsonObject("result").getAsJsonPrimitive("id").getAsString();
                        String assetId = EntryId;//body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

                        String inputFile = "mock/ovp." + service + "." + action + "." + assetId + (request.getBody().contains("startWidgetSession")? "" : ".ks")+".json";

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
