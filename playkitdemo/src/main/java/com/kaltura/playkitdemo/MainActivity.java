package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResultElement;
import com.kaltura.playkit.plugin.mediaprovider.mock.MockMediaProvider;
import com.kaltura.playkit.plugins.SamplePlugin;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PlayKit mPlayKit;
    private MockMediaProvider.Builder mockProviderBuilder;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        mockProviderBuilder = new MockMediaProvider.Builder().setFile("entries.playkit.json");
        MediaEntryProvider mediaEntryProvider = mockProviderBuilder.setId("m001").build();
        mediaEntryProvider.load(new OnCompletion<PKMediaEntry>() {
            @Override
            public void onComplete(PKMediaEntry response) {
                // loadPlayer()...
            }
        });

        // load another entry
        mockProviderBuilder.setId("m002").build().load(new OnCompletion<ResultElement>() {
            @Override
            public void onComplete(ResultElement response) {
                // loadPlayer()...
                if(response.isSuccess()) {
                    onMediaLoaded((PKMediaEntry) response.getResponse());
                }
            }
        });

    }

    private void onMediaLoaded(PKMediaEntry mediaEntry){
        mPlayKit = new PlayKit();

        PlayerConfig config = new PlayerConfig();
//        config.setAutoPlay(true);

        config.setMediaEntry(mediaEntry);
        config.enablePlugin("Sample");


        final Player player = mPlayKit.loadPlayer(this, config);

        Log.d(TAG, "Player: " + player.getClass());

        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {

            }
        });

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(player.getView());


        player.play();
    }
}
