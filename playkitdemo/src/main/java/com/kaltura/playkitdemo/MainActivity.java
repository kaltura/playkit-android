package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.mock.MockMediaProvider;

import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "MainActivity";

    private Player mPlayer;
    private PlaybackControlsView controlsView;
    private MockMediaProvider mockProvider;
    private JsonObject mockJsonData;
    private String[] mediaEntryNames = {"dash+xml", "mp4", "x-mpegURL"};
    private Spinner spinner;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        JsonParser parser = new JsonParser();

        mockJsonData = parser.parse(Utils.readAssetToString(this, "mock/entries.playkit.json")).getAsJsonObject();
        initSpinner();
    }

    private void initSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mediaEntryNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
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
            mPlayer.onResume();
        }
        if(controlsView != null){
            controlsView.resume();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mockProvider = new MockMediaProvider(mockJsonData, mediaEntryNames[position]);
        mockProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    onMediaLoaded(response.getResponse());
                }else{
                    Log.e(TAG, "failed to create a media entry " + response.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
