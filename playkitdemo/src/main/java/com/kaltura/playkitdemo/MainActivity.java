package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Player mPlayer;
    private PhoenixMediaProvider phoenixMediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    private MediaEntryProvider mediaEntryProvider;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        mediaEntryProvider = new MockMediaProvider("entries.playkit.json", this, "x-mpegURL");

//        phoenixMediaProvider = new PhoenixMediaProvider(MockParams.sessionProvider, MediaId, MockParams.MediaType, Format);
//
        mediaEntryProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse());
                        } else {

                            Toast.makeText(MainActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        });

    }

    private void onMediaLoaded(PKMediaEntry mediaEntry){

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);
        if(mPlayer == null){

            configurePlugins(config.plugins);


            mPlayer = PlayKitManager.loadPlayer(config, this);

            Log.d(TAG, "Player: " + mPlayer.getClass());

            addPlayerListeners();

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(mPlayer.getView());

            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(mPlayer);
        }else {
            mPlayer.prepare(config.media);
        }
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        JsonObject sampleConfig = new JsonObject();
        sampleConfig.addProperty("delay", 4200);
        config.setPluginConfig("Sample", sampleConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        controlsView.release();
        mPlayer.release();

    }

    private void addPlayerListeners() {

        mPlayer.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = true;
            }
        }, PlayerEvent.PLAY);

        mPlayer.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = false;
            }
        }, PlayerEvent.PAUSE);

        mPlayer.addStateChangeListener(new PKEvent.Listener<PlayerState.StateChangedEvent>() {
            @Override
            public void onEvent(PlayerState.StateChangedEvent event) {
                if(controlsView != null){
                    controlsView.setPlayerState(event.newState);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPlayer != null){
            mPlayer.restore();
            if (nowPlaying) {
                mPlayer.play();
            }
        }
        if(controlsView != null){
            controlsView.resume();
        }
    }
}
