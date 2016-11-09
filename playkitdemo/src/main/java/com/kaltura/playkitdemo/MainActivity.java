package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.plugins.SamplePlugin;

import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Player mPlayer;
    private PlaybackControlsView controlsView;
    private MockMediaEntryProvider mMediaEntryProvider;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        try {
            mMediaEntryProvider = new MockMediaEntryProvider().setInputJSONAsset(this, "entries.playkit.json").setMediaId("m001");
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Can't read config file", e);
            Toast.makeText(this, "JSON error: " + e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        
        super.onStart();
        
        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mMediaEntryProvider.getMediaEntry());
        config.plugins.enablePlugin("Sample");


        mPlayer = PlayKitManager.loadPlayer(config, this);
        
        Log.d(TAG, "Player: " + mPlayer.getClass());
        addPlayerListeners();

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(mPlayer.getView());

        controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
        controlsView.setPlayer(mPlayer);
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        mPlayer.release();
    }

    private void addPlayerListeners() {
        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if(controlsView != null){
                    controlsView.setPlayerState(newState);
                }
            }
        });
    }
}
