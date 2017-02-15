package com.kaltura.playkit.backend;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.OvpConfigs;
import com.kaltura.playkit.backend.ovp.OvpSessionProvider;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.backend.phoenix.APIDefines;
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

import java.util.concurrent.atomic.AtomicReference;

import static com.kaltura.playkit.backend.MockParams.FormatHD;
import static com.kaltura.playkit.backend.MockParams.FormatSD;
import static com.kaltura.playkit.backend.MockParams.MediaId;
import static com.kaltura.playkit.backend.MockParams.NonDRMEntryId;
import static com.kaltura.playkit.backend.MockParams.OvpBaseUrl;
import static com.kaltura.playkit.backend.MockParams.OvpLoginId;
import static com.kaltura.playkit.backend.MockParams.OvpPartnerId;
import static com.kaltura.playkit.backend.MockParams.OvpPassword;
import static com.kaltura.playkit.backend.MockParams.PnxBaseUrl;
import static com.kaltura.playkit.backend.MockParams.PnxPartnerId;
import static com.kaltura.playkit.backend.MockParams.PnxPassword;
import static com.kaltura.playkit.backend.MockParams.PnxUsername;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by tehilarozin on 28/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class SessionProviderAndroidTest extends BaseTest {

    @Test
    public void testOttSessionProviderBaseFlow() {
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(PnxBaseUrl, PnxPartnerId);

        ottSessionProvider.startSession(PnxUsername, PnxPassword, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error != null) {
                    Log.e("testOttSessionProvider", "failed to establish a session: " + response.error.getMessage());
                    resume();
                } else {
                    ottSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            new PhoenixMediaProvider()
                                    .setSessionProvider(ottSessionProvider)
                                    .setAssetId(MediaId)
                                    .setAssetType(APIDefines.KalturaAssetType.Media)
                                    .setFormats(FormatHD)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOttSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(MediaId));
                                                assertTrue(response.getResponse().getSources().size() == 1);
                                                assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));
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
    public void testOttAnonymousSession() {
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(PnxBaseUrl, PnxPartnerId);

        ottSessionProvider.startAnonymousSession(null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error != null) {
                    Log.e("testAnonymousSession", "failed to establish anonymous session: " + response.error.getMessage());
                    fail("Anonymous session creation failed: "+response.error.getMessage());

                } else {
                    ottSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            Log.e("testAnonymousSession", "get ks = " + response.getResult());
                            ottSessionProvider.endSession(new OnCompletion<BaseResult>() {
                                @Override
                                public void onComplete(BaseResult response) {
                                    assertTrue(response.error == null);

                                    new PhoenixMediaProvider()
                                            .setSessionProvider(ottSessionProvider)
                                            .setAssetId(MediaId)
                                            .setAssetType(APIDefines.KalturaAssetType.Media)
                                            .setFormats(FormatSD)
                                            .load(new OnMediaLoadCompletion() {
                                                @Override
                                                public void onComplete(ResultElement<PKMediaEntry> response) {
                                                    assertTrue(response != null && response.isSuccess());
                                                    Log.i("testOttSessionProvider", "we have mediaEntry");
                                                    assertTrue(response.getResponse().getId().equals(MediaId));
                                                    assertTrue(response.getResponse().getSources().size() == 1);
                                                    assertTrue(response.getResponse().getMediaType().equals(PKMediaEntry.MediaEntryType.Unknown));

                                                    resume();
                                                }
                                            });
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
    public void testOttEndSession() {
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(PnxBaseUrl, PnxPartnerId);

        ottSessionProvider.startSession(PnxUsername, PnxPassword, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error != null) {
                    Log.e("testAnonymousSession", "failed to establish anonymous session: " + response.error.getMessage());

                    if (response.error == ErrorElement.SessionError) {
                        fail("should have logged ");
                    }
                } else {
                    ottSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            Log.e("testAnonymousSession", "get ks = " + response.getResult());
                            ottSessionProvider.endSession(new OnCompletion<BaseResult>() {
                                @Override
                                public void onComplete(BaseResult response) {
                                    assertTrue(response.error == null);

                                    new PhoenixMediaProvider()
                                            .setSessionProvider(ottSessionProvider)
                                            .setAssetId(MediaId)
                                            .setAssetType(APIDefines.KalturaAssetType.Media)
                                            .setFormats(FormatHD)
                                            .load(new OnMediaLoadCompletion() {
                                                @Override
                                                public void onComplete(ResultElement<PKMediaEntry> response) {
                                                    assertTrue(response != null && response.getError() != null);
                                                    assertTrue(response.getError().equals(ErrorElement.SessionError));
                                                    resume();
                                                }
                                            });
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
                if (response.error != null) {
                    fail("failed to establish a session: " + response.error.getMessage());

                } else {
                    ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            new KalturaOvpMediaProvider()
                                    .setSessionProvider(ovpSessionProvider)
                                    .setEntryId(NonDRMEntryId)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOvpSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(NonDRMEntryId));
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
                if (response.error != null) {
                    Log.e("testAnonymousSession", "failed to establish anonymous session: " + response.error.getMessage());
                    fail("failed to create anonymous session: "+response.error.getMessage());

                } else {
                    ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            assertFalse(response.getResult().equals(""));

                            Log.e("testAnonymousSession", "get ks = " + response.getResult());
                            new KalturaOvpMediaProvider()
                                    .setSessionProvider(ovpSessionProvider)
                                    .setEntryId(NonDRMEntryId)
                                    .load(new OnMediaLoadCompletion() {
                                        @Override
                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                            if (response != null && response.isSuccess()) {
                                                Log.i("testOvpSessionProvider", "we have mediaEntry");
                                                assertTrue(response.getResponse().getId().equals(NonDRMEntryId));
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
                if (response.error != null) {
                    assertTrue(response.error.equals(ErrorElement.SessionError));
                    Log.i("testOvpSPError", response.error.getCode() + ", " + response.error.getMessage());

                    ovpSessionProvider.setBaseUrl("http://www.kaltura.whatever/api_v3/");
                    ovpSessionProvider.startSession(OvpLoginId, OvpPassword, OvpPartnerId, new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.error);
                            assertTrue(response.error.equals(ErrorElement.ConnectionError));
                            resume();
                        }
                    });
                }

            }
        });

        wait(1);
    }


    OvpSessionProvider ovpSessionProvider;

    @Test
    public void testOvpSP() {

        testOvpSessionProviderBaseFlow();

        testOvpUserEndSession();

    }


    String testKs;

    @Test
    public void testOvpUserEndSession() {
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        ovpSessionProvider = new OvpSessionProvider(OvpBaseUrl);
        ovpSessionProvider.startSession(OvpLoginId, OvpPassword, OvpPartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    ovpSessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
                        @Override
                        public void onComplete(PrimitiveResult response) {
                            assertNotNull(response.getResult());
                            testKs = response.getResult();

                            ovpSessionProvider.endSession(new OnCompletion<BaseResult>() {
                                @Override
                                public void onComplete(BaseResult response) {
                                    if (response.error == null) {
                                        APIOkRequestsExecutor.getSingleton().queue(BaseEntryService.list(ovpSessionProvider.baseUrl() + OvpConfigs.ApiPrefix,
                                                testKs, NonDRMEntryId)
                                                .completion(new OnRequestCompletion() {
                                                    @Override
                                                    public void onComplete(ResponseElement response) {
                                                        try {
                                                            assertNotNull(response.getResponse());
                                                            BaseResult parsedResponse = PhoenixParser.parse(response.getResponse());
                                                            assertNotNull(parsedResponse);
                                                            assertNotNull(parsedResponse.error);
                                                            assertTrue(parsedResponse.error.getCode().toLowerCase().contains("invalid_ks"));

                                                            new KalturaOvpMediaProvider()
                                                                    .setSessionProvider(ovpSessionProvider)
                                                                    .setEntryId(NonDRMEntryId)
                                                                    .load(new OnMediaLoadCompletion() {
                                                                        @Override
                                                                        public void onComplete(ResultElement<PKMediaEntry> response) {
                                                                            //after ending session, it can't be renewed, start session should be called.
                                                                            try {
                                                                                assertNotNull(response.getError());
                                                                                assertTrue(response.getError().equals(ErrorElement.SessionError));
                                                                            } catch (AssertionError e) {
                                                                                failure.set(e);
                                                                                fail("failed assert error on entry loading on expired ks: " + e.getMessage());
                                                                            } finally {
                                                                                resume();
                                                                            }
                                                                        }
                                                                    });
                                                        } catch (AssertionError error) {
                                                            failure.set(error);
                                                            fail("failed assert error on expired ks: " + error.getMessage());
                                                        }
                                                    }
                                                }).build());

                                    } else {
                                        PKLog.i(TAG, "got an error: " + response.error.getMessage());
                                        fail("failed to end session");
                                        resume();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    fail("failed to establish session");
                }
            }
        });
        wait(1);
    }

    //TODO add failure test, add test for expiration - check renewal of session
}
