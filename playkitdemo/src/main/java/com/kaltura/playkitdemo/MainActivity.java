package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.mock.MockMediaProvider;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //private PlayKit mPlayKit;
    private MockMediaProvider mockProvider;
    private Player mPlayer;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        mockProvider = new MockMediaProvider("entries.playkit.json", this, "1_1h1vsv3z");

    }

    @Override
    protected void onStart() {
        super.onStart();
        mockProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    onMediaLoaded(response.getResponse());
                }
            }

        });
    }

    private void onMediaLoaded(PKMediaEntry mediaEntry){

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);
        configurePlugins(config.plugins);


        mPlayer = PlayKitManager.loadPlayer(config, this);

        Log.d(TAG, "Player: " + mPlayer.getClass());

        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {

            }
        });

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(mPlayer.getView());


        mPlayer.play();
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        try {
            config.setPluginConfig("Sample", new JSONObject().put("delay", 4200));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mPlayer.release();
    }
}
