package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Plugin;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.plugins.SamplePlugin;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainActivity";

    private Player player;

    private ImageButton btnPlay, btnPause, btnFastForward, btnRewind, btnNext, btnPrevious;
    private SeekBar seekBar;
    private TextView tvCurTime, tvTime;
    private Plugin.Factory samplePlugin;


    private void registerPlugins() {
        PlayKitManager.registerPlugin(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPlaybackControls();

        registerPlugins();


        JSONObject configJSON = prepareConfigJSON();
        initPlayer();


        PlayerConfig config = new PlayerConfig();
        config.setShouldAutoPlay(true);

        try {
            MockMediaEntryProvider mediaEntryProvider = new MockMediaEntryProvider(configJSON);
            mediaEntryProvider.loadMediaEntry("m001");
            config.setEntry(mediaEntryProvider.getMediaEntry());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load media info", e);
        }

        player.load(config);

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(player.getView());

    }

    private JSONObject prepareConfigJSON() {
        JSONObject configJSON = null;
        try {
            configJSON = new JSONObject(Utils.readAssetToString(this, "entries.playkit.json"));
        } catch (JSONException e) {
            Log.e(TAG, "Can't read config file", e);
        }

        return configJSON;
    }

    private void initPlayer() {
        player = PlayKitManager.createPlayer(this);
        //add player listeners.
        player.addBoundaryTimeListener(new Player.TimeListener() {
            @Override
            public void onTimeReached(Player player, Player.RelativeTime.Origin origin, long offset) {
                Log.e(TAG, "onTimeReached => ");

            }
        }, true, Player.RelativeTime.START);

        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                Log.e(TAG, "state changed to => " + newState.name());
            }
        });
    }

    private void initPlaybackControls() {
        btnPlay = (ImageButton) this.findViewById(R.id.play);
        btnPause = (ImageButton) this.findViewById(R.id.pause);
        btnFastForward = (ImageButton) this.findViewById(R.id.ffwd);
        btnRewind = (ImageButton) this.findViewById(R.id.rew);
        btnNext = (ImageButton) this.findViewById(R.id.next);
        btnPrevious = (ImageButton) this.findViewById(R.id.prev);

        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnFastForward.setOnClickListener(this);
        btnRewind.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);

        seekBar = (SeekBar) this.findViewById(R.id.mediacontroller_progress);
        seekBar.setOnSeekBarChangeListener(this);

        tvCurTime = (TextView) this.findViewById(R.id.time_current);
        tvTime = (TextView) this.findViewById(R.id.time);

        LinearLayout controlsLayout = (LinearLayout) this.findViewById(R.id.playerControls);
//        controlsLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                player.play();
                break;
            case R.id.pause:
                player.pause();
                break;
            case R.id.ffwd:
                //Do nothing
                break;
            case R.id.rew:
                //Do nothing
                break;
            case R.id.next:
                //Do nothing
                break;
            case R.id.prev:
                //Do nothing
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
