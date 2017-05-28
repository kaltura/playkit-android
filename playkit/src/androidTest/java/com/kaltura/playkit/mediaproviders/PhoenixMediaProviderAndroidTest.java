package com.kaltura.playkit.mediaproviders;

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
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
import com.kaltura.playkit.api.phoenix.APIDefines;
import com.kaltura.playkit.api.phoenix.PhoenixParser;
import com.kaltura.playkit.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.kaltura.playkit.mediaproviders.MockParams.ChannelId;
import static com.kaltura.playkit.mediaproviders.MockParams.FormatHD;
import static com.kaltura.playkit.mediaproviders.MockParams.FormatHDDash;
import static com.kaltura.playkit.mediaproviders.MockParams.FormatSD;
import static com.kaltura.playkit.mediaproviders.MockParams.FrozenAssetInfo;
import static com.kaltura.playkit.mediaproviders.MockParams.MediaId;
import static com.kaltura.playkit.mediaproviders.MockParams.ShlomoArMediaId;
import static com.kaltura.playkit.mediaproviders.MockParams.MediaId2_File_Main_HD;
import static com.kaltura.playkit.mediaproviders.MockParams.MediaId2_File_Main_SD;
import static com.kaltura.playkit.mediaproviders.MockParams.MediaId5;
import static com.kaltura.playkit.mediaproviders.MockParams.PnxBaseUrl;
import static com.kaltura.playkit.mediaproviders.MockParams.PnxKS;
import static com.kaltura.playkit.mediaproviders.MockParams.PnxNotEntitledMedia;
import static com.kaltura.playkit.mediaproviders.MockParams.PnxPartnerId;
import static com.kaltura.playkit.mediaproviders.MockParams.ToystoryMediaId;
import static com.kaltura.playkit.mediaproviders.MockParams.Toystory_File_Main_HD;
import static com.kaltura.playkit.mediaproviders.MockParams.Toystory_File_Main_HD_Dash;
import static com.kaltura.playkit.mediaproviders.MockParams.Toystory_File_SD_Dash;
import static com.kaltura.playkit.mediaproviders.MockParams.WebHD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class PhoenixMediaProviderAndroidTest extends BaseTest {

    private RequestQueue testExecutor;
    private PhoenixMediaProvider phoenixMediaProvider;
    private int latchCount;

    private SessionProvider ksSessionProvider = new SessionProvider() {
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

    private SessionProvider InvalidSessionProvider = new SessionProvider() {
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
                setAssetId(ShlomoArMediaId).setAssetType(APIDefines.KalturaAssetType.Media)/*.setFormats(FormatSD)*//*.setRequestExecutor(testExecutor)*/;

        latchCount = 1;
        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                latchCount--;
                assertTrue(response.isSuccess());
                assertTrue(response.getResponse() != null);
                assertTrue(response.getResponse().getId().equals(ShlomoArMediaId));
                assertTrue(response.getResponse().getSources().size() == 2);
                assertTrue(response.getResponse().getDuration() == 224000);
                // currently is unknown on phoenix since we don't have that information easily:
                assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                latchCount++;
                phoenixMediaProvider.setAssetId(ToystoryMediaId).setFormats(FormatHDDash, FormatSD).setRequestExecutor(APIOkRequestsExecutor.getSingleton()).load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(ToystoryMediaId));
                            assertTrue(response.getResponse().getSources().size() == 1);
                            assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                        } else {
                            assertNotNull(response.getError());
                            Log.e("PhoenixMediaProvider", "asset can't be played: " + response.getError().getMessage());
                        }

                        PhoenixMediaProviderAndroidTest.this.resume();
                        latchCount--;
                    }
                });
            }
        });

        wait(latchCount);

    }


    @Test
    public void testPlaybackSourcesByFormats() {
        latchCount = 0;
        final SessionProvider EmptySessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return PnxBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(""));
                }
            }

            @Override
            public int partnerId() {
                return PnxPartnerId;
            }
        };
        latchCount++;
        EmptySessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                latchCount--;
                phoenixMediaProvider = new PhoenixMediaProvider()
                        .setSessionProvider(EmptySessionProvider)
                        .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ToystoryMediaId)
                        .setFormats(FormatHD, FormatSD, FormatHDDash);

                latchCount++;
                phoenixMediaProvider.load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(ToystoryMediaId));
                            assertTrue(response.getResponse().getSources().size() == 1);
                            assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                        } else {
                            assertNotNull(response.getError());
                            fail("asset can't be played: " + response.getError().getMessage());
                            resume();
                        }

                        latchCount--;
                        PhoenixMediaProviderAndroidTest.this.resume();

                    }
                });

                PhoenixMediaProviderAndroidTest.this.wait(latchCount);
            }
        });
    }

    @Test
    public void testFailMessagesOnPlaybackSources() {
        latchCount = 1;
        ksSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                latchCount--;
                phoenixMediaProvider = new PhoenixMediaProvider()
                        .setSessionProvider(ksSessionProvider)
                        .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(PnxNotEntitledMedia)
                        .setFormats(FormatHD, FormatSD, WebHD);

                latchCount++;
                phoenixMediaProvider.load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        assertFalse(response.isSuccess());
                        assertNull(response.getResponse());
                        assertNotNull(response.getError());
                        assertTrue(response.getError() instanceof RestrictionError);
                        assertTrue(((RestrictionError) response.getError()).getExtra().equals(RestrictionError.Restriction.NotEntitled));

                        latchCount--;

                        PhoenixMediaProviderAndroidTest.this.resume();

                    }
                });
            }
        });

        PhoenixMediaProviderAndroidTest.this.wait(latchCount);
    }

    @Test
    public void testPlaybackSourcesByMediaFiles() {
        latchCount = 0;
        final SessionProvider EmptySessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return PnxBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(""));
                }
            }

            @Override
            public int partnerId() {
                return PnxPartnerId;
            }
        };
        latchCount++;
        EmptySessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                latchCount--;
                phoenixMediaProvider = new PhoenixMediaProvider()
                        .setSessionProvider(EmptySessionProvider)
                        .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ToystoryMediaId)
                        .setFileIds(Toystory_File_Main_HD_Dash, Toystory_File_SD_Dash, Toystory_File_Main_HD);
                latchCount++;
                phoenixMediaProvider.load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            assertTrue(response.getResponse() != null);
                            assertTrue(response.getResponse().getId().equals(ToystoryMediaId));
                            List<PKMediaSource> sources = response.getResponse().getSources();
                            assertNotNull(sources);
                            assertTrue(sources.size() == 1);
                            for (PKMediaSource source : sources) {
                                if (source.getId().equals(Toystory_File_Main_HD_Dash)) {
                                    assertTrue(source.getMediaFormat().equals(PKMediaFormat.dash));
                                }
                                /*if (source.getId().equals(Toystory_File_SD_Dash)) {
                                    assertTrue(source.getMediaFormat().equals(PKMediaFormat.dash));
                                }
                                if (source.getId().equals(Toystory_File_Main_HD)) {
                                    assertTrue(source.getMediaFormat().equals(PKMediaFormat.wvm));
                                }*/
                            }
                            assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                        } else {
                            assertNotNull(response.getError());
                            fail("asset can't be played: " + response.getError().getMessage());
                            resume();
                        }
                        latchCount--;

                        PhoenixMediaProviderAndroidTest.this.resume();

                    }
                });

                PhoenixMediaProviderAndroidTest.this.wait(latchCount);

            }
            // }
        });
    }

    @Test
    public void testFormatsFilesLoadCompare() {

        final List<PKMediaSource> sources = new ArrayList<>();

        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ShlomoArMediaId) // cannel considered as media
                .setFormats(FormatHD, FormatSD);

        latchCount = 1; // counts total asynchronous section latch should wait for, prevents test stack on wait.

        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertNotNull(response.getResponse());
                    if (sources.size() > 0) {
                        validateSources(sources, response.getResponse().getSources());
                    } else {
                        synchronized (sources) {
                            sources.addAll(response.getResponse().getSources());
                        }
                    }
                }
                latchCount--;
                PhoenixMediaProviderAndroidTest.this.resume();
            }
        });

        PhoenixMediaProvider phoenixMediaProvider2 = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ShlomoArMediaId) // cannel considered as media
                .setFileIds(MediaId2_File_Main_HD, MediaId2_File_Main_SD);

        latchCount++;
        phoenixMediaProvider2.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertNotNull(response.getResponse());
                    if (sources.size() > 0) {
                        validateSources(sources, response.getResponse().getSources());
                    } else {
                        synchronized (sources) {
                            sources.addAll(response.getResponse().getSources());
                        }
                    }
                }

                latchCount--;
                PhoenixMediaProviderAndroidTest.this.resume();
            }
        });

        PhoenixMediaProviderAndroidTest.this.wait(latchCount);
    }

    private void validateSources(List<PKMediaSource> sources1, List<PKMediaSource> sources2) {
        assertTrue(sources1.size() == sources2.size());
        int compCount = sources1.size();

        for (PKMediaSource source1 : sources1) {
            for (PKMediaSource source2 : sources2) {
                if (source1.getId().equals(source2.getId())) {
                    assertTrue(source1.getMediaFormat().equals(source2.getMediaFormat()));
                    assertTrue(source1.hasDrmParams() == source2.hasDrmParams());
                    if (source1.hasDrmParams()) {
                        assertTrue(source2.getDrmData().size() == source2.getDrmData().size());
                    }
                    compCount--;
                }
            }
        }

        assertTrue(compCount == 0);
    }

    @Test
    public void testLiveRemoteLoading() {
        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ChannelId) // cannel considered as media
                .setFormats(FormatHD, FormatSD);

        latchCount = 1;
        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(ChannelId));
                    // as response to the API, we get 2 sources one of them is "Web HD", the other can be played
                    // by android, and the source we get here in the PKMediaEntry
                    assertTrue(response.getResponse().getSources().size() == 1);
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                } else {
                    assertNotNull(response.getError());
                    Log.e("PhoenixMediaProvider", "asset can't be played: " + response.getError());
                }

                latchCount--;
                PhoenixMediaProviderAndroidTest.this.resume();

            }
        });

        wait(latchCount);
    }

    @Test
    public void testLoadCancel() {

        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(MediaId5)
                .setFormats(FormatHD, FormatSD);

        int wait = 1;
        loadCancelTest1();
        wait--;
        wait(wait);

        wait = 1;
        loadCancelTest2(true);
        wait--;
        wait(wait);
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
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

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
        phoenixMediaProvider.setAssetId(ShlomoArMediaId).setFormats(FormatHD, FormatSD).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (cancelAtEnd) {
                    PKLog.e("phoenix testing", "load completion on a canceled load 3");
                    fail("load 3 should have been canceled");
                    resume();
                } else {
                    assertTrue(response.getResponse() != null);
                    assertTrue(response.getResponse().getId().equals(ShlomoArMediaId));
                    assertTrue(response.getResponse().getSources().size() == 2);
                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));
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
            assertTrue(asset instanceof KalturaMediaAsset);
            //mediaEntry = PhoenixMediaProvider.getMediaEntry((KalturaMediaAsset) asset, Arrays.asList(FormatHD, FormatSD));

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(mediaEntry);
        assertEquals(2, mediaEntry.getSources().size());

    }

    @Test
    public void testPKMediaEntryParceling() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        latchCount = 1;

        phoenixMediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetType(APIDefines.KalturaAssetType.Media).setAssetId(ToystoryMediaId) // cannel considered as media
                .setFormats(FormatSD, FormatHDDash);

        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        final PKMediaEntry baseMedia = response.getResponse();
                        Bundle mediaBundle = new Bundle();
                        mediaBundle.putParcelable("mediatest", baseMedia);

                                    /*Parcel parcel = Parcel.obtain();
                                    parcel.writeTypedList(baseMedia.getSources().get(0).getDrmData());
                                    parcel.setDataPosition(0);
                                    List<PKDrmParams> drmParamses = parcel.createTypedArrayList(PKDrmParams.CREATOR);*/

                        PKMediaEntry newMedia = mediaBundle.getParcelable("mediatest");
                        assertNotNull(newMedia);
                        assertEquals(baseMedia.hasSources(), newMedia.hasSources());
                        assertEquals(baseMedia.getSources().size(), newMedia.getSources().size());
                        assertEquals(baseMedia.getSources().get(0).hasDrmParams(), newMedia.getSources().get(0).hasDrmParams());

                        assertEquals(baseMedia.getSources().get(0).getDrmData().size(), newMedia.getSources().get(0).getDrmData().size());
                    }
                } catch (AssertionError ae) {
                    failure.set(ae);
                }
                latchCount--;
                PhoenixMediaProviderAndroidTest.this.resume();
            }
        });

        wait(latchCount);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testInvalidSession() {
        new PhoenixMediaProvider().setSessionProvider(InvalidSessionProvider).setAssetId(MediaId).setAssetType(APIDefines.KalturaAssetType.Media).setFormats(FormatHD).load(new OnMediaLoadCompletion() {
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

            static final String SERVICE = "/service/";
            static final String ACTION = "/action/";
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

