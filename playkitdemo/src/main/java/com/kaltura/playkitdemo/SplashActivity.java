package com.kaltura.playkitdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.gson.Gson;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkitdemo.data.JsonTask;
import com.kaltura.playkitdemo.jsonConverters.ConverterPlayKitApp;

import java.util.Date;

public class SplashActivity extends AppCompatActivity {


    public static final String CONVERTER_PLAY_KIT_APP = "CONVERTER_PLAY_KIT_APP";
    private static final int DELAY_SHOW_SPLASH_SCREEN = 2000;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        registerPlugins();

        Intent intent = getIntent();
        String url;

        if ((url = intent.getDataString()) != null) { // app was invoked via deep linking
            handleDeepLink(url);
        } else { // app invocation done by the standard way

        }
    }



    private void handleDeepLink(String url) {

        final Date startTime = new Date();

        new JsonTask(SplashActivity.this, new JsonTask.OnJsonFetchedListener() {

            @Override
            public void onJsonFetched(String json) {

                final ConverterPlayKitApp playKitApp = new Gson().fromJson(json, ConverterPlayKitApp.class);

                Date currentTime = new Date();
                final long diffTime = currentTime.getTime() - startTime.getTime();


                Thread thread = new Thread(new Runnable() {

                    public void run() {

                        try {
                            // we want to show the splash screen for at least 2 seconds
                            long sleepTime = DELAY_SHOW_SPLASH_SCREEN - diffTime;
                            Thread.sleep(sleepTime > 0 ? sleepTime : 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.putExtra(CONVERTER_PLAY_KIT_APP, playKitApp);
                        startActivity(intent);
                    }
                });

                thread.start();

            }
        }).execute(url);
    }

}
