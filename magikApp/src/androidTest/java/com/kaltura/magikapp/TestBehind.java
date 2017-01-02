package com.kaltura.magikapp;

import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.connect.backend.PrimitiveResult;
import com.connect.backend.magikapp.data.Configuration;
import com.connect.backend.magikapp.services.ConfigurationService;
import com.connect.backend.phoenix.services.AssetService;
import com.connect.core.OnCompletion;
import com.connect.utils.APIOkRequestsExecutor;
import com.connect.utils.OnRequestCompletion;
import com.connect.utils.RequestBuilder;
import com.connect.utils.ResponseElement;
import com.google.gson.Gson;
import com.kaltura.playkit.backend.phoenix.OttSessionProvider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.kaltura.magikapp.MockParams.OttPartnerId;
import static com.kaltura.magikapp.MockParams.PhoenixBaseUrl;
import static junit.framework.Assert.fail;

/**
 * Created by tehilarozin on 01/01/2017.
 */

@RunWith(AndroidJUnit4.class)
public class TestBehind extends BaseTest {

    @Test
    public void testConfigurationFetch(){
        RequestBuilder requestBuilder = ConfigurationService.fetch(BuildConfig.APPLICATION_ID.replace(".", "-")).completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response.isSuccess()){
                    Configuration configuration = new Gson().fromJson(response.getResponse(), Configuration.class);
                    Assert.assertNull(configuration.error);
                    Assert.assertTrue(!TextUtils.isEmpty(configuration.getIcon()));
                    Assert.assertTrue(!TextUtils.isEmpty(configuration.getLogo()));
                    Assert.assertTrue(!TextUtils.isEmpty(configuration.getName()));
                    Assert.assertTrue(!TextUtils.isEmpty(configuration.getPrimaryClr()));
                    Assert.assertNotNull(configuration.getMenu());
                    Assert.assertTrue(configuration.getMenu().size() > 0);
                }
                resume();
            }
        });
        APIOkRequestsExecutor.getSingleton().queue(requestBuilder.build());
        wait(1);
    }

    @Test
    public void testChannelContentFetch(){
        OttSessionProvider ottSessionProvider = new OttSessionProvider(PhoenixBaseUrl, OttPartnerId);
        ottSessionProvider.startAnonymousSession(null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error != null){
                    fail("failed to get channels content ");
                }
                RequestBuilder requestBuilder = AssetService.listByChannel(PhoenixBaseUrl, response.getResult(), 123456, null ).completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        if(response.isSuccess()){
                            Configuration configuration = new Gson().fromJson(response.getResponse(), Configuration.class);
                            Assert.assertNull(configuration.error);
                            Assert.assertTrue(!TextUtils.isEmpty(configuration.getIcon()));
                            Assert.assertTrue(!TextUtils.isEmpty(configuration.getLogo()));
                            Assert.assertTrue(!TextUtils.isEmpty(configuration.getName()));
                            Assert.assertTrue(!TextUtils.isEmpty(configuration.getPrimaryClr()));
                            Assert.assertNotNull(configuration.getMenu());
                            Assert.assertTrue(configuration.getMenu().size() > 0);
                        }
                        resume();
                    }
                });
                APIOkRequestsExecutor.getSingleton().queue(requestBuilder.build());
            }
        });

        wait(1);
    }
}
