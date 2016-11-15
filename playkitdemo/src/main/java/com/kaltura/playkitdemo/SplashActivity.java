package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {


    private static final int DELAY_SHOW_SPLASH_SCREEN = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        fetchAppData();
    }




    private void fetchAppData() {


        Thread thread = new Thread(new Runnable() {

            public void run() {

                try {
                    Thread.sleep(DELAY_SHOW_SPLASH_SCREEN);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        thread.start();

    }



}
