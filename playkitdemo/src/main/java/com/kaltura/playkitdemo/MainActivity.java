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

import java.util.Formatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainActivity";
    private static final int PROGRESS_BAR_MAX = 100;
    private static final long TIME_UNSET = Long.MIN_VALUE + 1;

    private PlayKit mPlayKit;
    private MockMediaEntryProvider mMediaEntryProvider;


    private Player player;
    private PlayerState playerState;

    private LinearLayout controlsLayout;
    private ImageButton btnPlay, btnPause, btnFastForward, btnRewind, btnNext, btnPrevious;
    private SeekBar seekBar;
    private TextView tvCurTime, tvTime;
    private boolean dragging = false;
    private StringBuilder formatBuilder;
    private Formatter formatter;

    private Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };




    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPlaybackControls();
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


        player = mPlayKit.createPlayer(this, config);

        Log.d(TAG, "Player: " + player.getClass());

        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                playerState = newState;
                updateProgress();
            }
        });

        LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
        layout.addView(player.getView());
        updateProgress();
    }

    private void initPlaybackControls() {
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

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

        controlsLayout = (LinearLayout) this.findViewById(R.id.playerControls);
    }

    private void updateProgress() {

        long duration = player == null ? 0 : player.getDuration();
        long position = player == null ? 0 : player.getCurrentPosition();
        tvTime.setText(stringForTime(duration));
        if (!dragging) {
            tvCurTime.setText(stringForTime(position));
        }
        if (!dragging) {
            seekBar.setProgress(progressBarValue(position));
        }
        long bufferedPosition = player == null ? 0 : player.getBufferedPosition();
        seekBar.setSecondaryProgress(progressBarValue(bufferedPosition));
        // Remove scheduled updates.
        controlsLayout.removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        if (playerState != PlayerState.IDLE) {
            long delayMs;
            if (player.getAutoPlay() && playerState == PlayerState.READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            controlsLayout.postDelayed(updateProgressAction, delayMs);
        }
    }

    private int progressBarValue(long position) {
        long duration = player == null ? TIME_UNSET : player.getDuration();
        return duration == TIME_UNSET || duration == 0 ? 0
                : (int) ((position * PROGRESS_BAR_MAX) / duration);
    }

    private long positionValue(int progress) {
        long duration = player == null ? TIME_UNSET : player.getDuration();
        return duration == TIME_UNSET ? 0 : ((duration * progress) / PROGRESS_BAR_MAX);
    }

    private String stringForTime(long timeMs) {
        if (timeMs == TIME_UNSET) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
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


    public void onStartTrackingTouch(SeekBar seekBar) {
        dragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        dragging = false;
        player.seekTo(positionValue(seekBar.getProgress()));
    }
}
