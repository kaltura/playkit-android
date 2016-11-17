package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;

import org.json.JSONException;
import org.json.JSONObject;

import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.MediaId;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Player mPlayer;
    private PhoenixMediaProvider phoenixMediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();


        //mockProvider = new MockMediaProvider("mock/entries.playkit.json", this, "1_1h1vsv3z");
        phoenixMediaProvider = new PhoenixMediaProvider(MockParams.sessionProvider, MediaId, MockParams.MediaType, Format);



    }

    @Override
    protected void onStart() {
        super.onStart();


        /*
        mockProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    onMediaLoaded(response.getResponse());
                }
            }

        });
        */



        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
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
        try {
            config.setPluginConfig("Sample", new JSONObject().put("delay", 4200));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        controlsView.release();
        mPlayer.release();

    }

    private void addPlayerListeners() {
        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);
        
        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                switch (event) {
                    case PLAY:
                        nowPlaying = true;
                        break;
                    case PAUSE:
                        nowPlaying = false;
                        break;
                }
            }
        }, PlayerEvent.PLAYING);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if(controlsView != null){
                    controlsView.setPlayerState(newState);
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