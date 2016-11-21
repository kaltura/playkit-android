package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.plugins.SamplePlugin;

import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.MediaId;
import static com.kaltura.playkitdemo.MockParams.MediaId3;
import static com.kaltura.playkitdemo.MockParams.sessionProvider;


public class StandalonePlayerActivity extends AppCompatActivity {

    private Player mPlayer;
    private LinearLayout mPlayerContainer;
    private PlaybackControlsView mControlsView;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standalone_player);

        mPlayerContainer = (LinearLayout) findViewById(R.id.player_root);
        mControlsView = (PlaybackControlsView) findViewById(R.id.playerControls);

        registerPlugins();

        Intent intent = getIntent();
        configurePlayer(intent);

    }


    private void configurePlayer(Intent intent) {

        String url;

        if ((url = intent.getDataString()) != null) { // app was invoked via deep linking

            PlayerProvider.getPlayer(url, StandalonePlayerActivity.this, new PlayerProvider.OnPlayerReadyListener() {
                @Override
                public void onPlayerReady(Player player) {
                    mPlayer = player;
                    startPlay();
                }
            });

        } else { // app invocation done by the standard way

            MediaEntryProvider mediaEntryProvider = new PhoenixMediaProvider(sessionProvider, MediaId3, MockParams.MediaType, Format);

            PlayerProvider.getPlayer(mediaEntryProvider, StandalonePlayerActivity.this, new PlayerProvider.OnPlayerReadyListener() {
                @Override
                public void onPlayerReady(Player player) {
                    mPlayer = player;
                    startPlay();
                }
            });
        }
    }

    private void startPlay() {

        if (mPlayer == null) {
            return;
        }

        mPlayerContainer.addView(mPlayer.getView());

        mControlsView.setPlayer(mPlayer);
        mControlsView.resume();

        //mPlayer.play();
    }



    @Override
    protected void onResume() {
        super.onResume();

        if (mPlayer != null) {
            mPlayer.restore();
            mControlsView.resume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mPlayer != null) {
            mPlayer.release();
            mControlsView.release();
        }
    }


}

