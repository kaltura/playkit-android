package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.plugins.connect.ResultElement;
import com.kaltura.playkit.plugins.mediaprovider.base.OnMediaLoadCompletion;
import com.kaltura.playkit.plugins.mediaprovider.mock.MockMediaProvider;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PlayKit mPlayKit;
    private MockMediaProvider mockProvider;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        mockProvider = new MockMediaProvider("entries.playkit.json", "1_1h1vsv3z");
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
