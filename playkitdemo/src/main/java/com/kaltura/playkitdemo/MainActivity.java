package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.Entries;
import com.kaltura.playkit.MediaEntry;
import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerFactory;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    JSONObject mConfigJSON;
    Entries mEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            //mConfigJSON = new JSONObject(Utils.readAssetToString(this, "entries.playkit.json"));
            //mEntries = new Gson().fromJson()
            MockMediaEntryProvider mockMediaEntryProvider = new MockMediaEntryProvider("entries.playkit.json")

        } catch (JSONException e) {
            Log.e(TAG, "Can't read config file", e);
            return;
        }
        
        
        final Player player = PlayerFactory.newPlayer(this);
        
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
        
        PlayerConfig config = getPlayerConfig(0);

        if (config != null) {
            config.shouldAutoPlay = false;
            player.load(config);

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(player.getView());

            player.play();
        }
    }

    private PlayerConfig getPlayerConfig(int index) {
        try {
            JSONArray jsonArray = mConfigJSON.getJSONArray("entries");
            JSONObject jsonEntry = jsonArray.getJSONObject(index);
            MediaEntry mediaEntry = new MediaEntry(jsonEntry);
            PlayerConfig playerConfig = new PlayerConfig();
            playerConfig.entry = mediaEntry;
            return playerConfig;
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
            return null;
        }
    }
}
