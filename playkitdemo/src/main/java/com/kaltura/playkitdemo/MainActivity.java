package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.plugins.SamplePlugin;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private PlayKit mPlayKit;
    private MockMediaEntryProvider mMediaEntryProvider;


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        registerPlugins();

        JSONObject configJSON;
        try {
            configJSON = new JSONObject(Utils.readAssetToString(this, "entries.playkit.json"));
            mMediaEntryProvider = new MockMediaEntryProvider(configJSON);
        } catch (JSONException e) {
            Log.e(TAG, "Can't read config file", e);
            Toast.makeText(this, "JSON error: " + e, Toast.LENGTH_LONG).show();
            return;
        }

        mPlayKit = new PlayKit();

        PlayerConfig config = new PlayerConfig();
//        config.setAutoPlay(true);

        mMediaEntryProvider.loadMediaEntry("m001");
        config.setMediaEntry(mMediaEntryProvider.getMediaEntry());
        config.getPluginConfig("Sample");


        final Player player = mPlayKit.createPlayer(this, config);
        
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
