package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.PlayKit;
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
    private PlayKit mPlayKit;
    private MockMediaEntryProvider mMediaEntryProvider;


    private Player player;

    private ImageButton btnPlay, btnPause, btnFastForward, btnRewind, btnNext, btnPrevious;
    private SeekBar seekBar;
    private TextView tvCurTime, tvTime;
    private Plugin.Factory samplePlugin;


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

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(player.getView());


        player.play();
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
