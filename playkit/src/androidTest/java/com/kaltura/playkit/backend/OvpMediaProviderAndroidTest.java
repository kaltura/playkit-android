package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.KalturaOvpParser;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class OvpMediaProviderAndroidTest extends BaseTest {

    public static final String BaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage
    public static final String KS = "djJ8MjIwOTU5MXxPFV50RZYaUMbON6FvA-1iQPTPBslap3ZAXFeFq-lMcCPZsUg0Y4vGbd769dmH1lDRq-nWMe1XhOTIVhUgk-V3exyHva1QkOcwtxR6bAm9sRZD2tQrLo3r-0VqmLMWRjU=";
    public static final String EntryId = "1_1h1vsv3z";
    public static final String EntryId2 = "1_ztdp5s5d";
    public static final String EntryId3 = "0_tb83i9pr";
    public static final int PartnerId = 2209591;


    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return BaseUrl;
        }

        @Override
        public void getKs(OnCompletion<String> completion) {
            if(completion != null){
                completion.onComplete(KS);
            }
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
    public void testEntryInfoLiveFetch(){
        new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(EntryId3).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(EntryId3));
                    assertTrue(response.getResponse().getSources().size() == 4);
                    //assertTrue(response.getResponse().getDuration() == 102000);

                } else {
                    assertNotNull(response.getError());
                }

                OvpMediaProviderAndroidTest.this.resume();
            }
        });

        wait(1);
    }


    @Test
    public void testMockVsLiveRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(EntryId3).setRequestExecutor(testExecutor);
        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                assertTrue(response.isSuccess());
                assertTrue(response.getResponse() != null);
                assertTrue(response.getResponse().getId().equals(EntryId3));
                assertTrue(response.getResponse().getSources().size() == 3); // format "hdnetworkmanifest" is excluded
                assertTrue(response.getResponse().getDuration() == 102000);

                /*kalturaOvpMediaProvider.setRequestExecutor(APIOkRequestsExecutor.getSingleton()).load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(EntryId));
                            assertTrue(response.getResponse().getSources().size() == 5);
                            assertTrue(response.getResponse().getDuration() == 102000);

                        } else {
                            assertNotNull(response.getError());

                        }

                        OvpMediaProviderAndroidTest.this.resume();
                    }
                });*/
            }
        });

        wait(1);
    }

    @Test
    public void testErrorHandling() {
        /* TODO:
        * invalid ks
        * invalid assetid
        * wrong server url
        * invalid response structure
        * check server error object handling*/

        String multiresponseWithError= "[\n" +
                "  {\n" +
                "    \"partnerId\": 2209591,\n" +
                "    \"ks\": \"djJ8MjIwOTU5MXwKyMTAkPT_yYS2zVNweHck77yCGu25hdaflLaUwwjFSrF-7gQD9Z0xFC_g6o7HySRqSsPM5bU8Y8VpunwR4K4dxfuv10aQE8gG6lbQg4RZGA==\",\n" +
                "    \"userId\": 0,\n" +
                "    \"objectType\": \"KalturaStartWidgetSessionResponse\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"code\": \"ENTRY_ID_NOT_FOUND\",\n" +
                "    \"message\": \"Entry id \\\"1_1h1vsv3z\\\" not found\",\n" +
                "    \"objectType\": \"KalturaAPIException\",\n" +
                "    \"args\": {\n" +
                "      \"ENTRY_ID\": \"1_1h1vsv3z\"\n" +
                "    }\n" +
                "  }\n" +
                "]";
    }

    @Test
    public void testPrimitiveResponseParsing(){
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
        assertTrue(((BaseResult)parsed).error != null);
        assertTrue(((BaseResult)parsed).error.getCode().equals("USER_WRONG_PASSWORD"));
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
        public String queue(RequestElement requestElement) {
            new RequestHandler(requestElement).run();
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
                    String action = actionIdx == -1 ? "_" : url.substring(actionIdx + ACTION.length());

                    if (request.getBody() != null) {
                        JsonParser parser = new JsonParser();
                        JsonObject body = parser.parse(request.getBody()).getAsJsonObject();
                        String assetId = "";
                        if(body.has("2")){
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
