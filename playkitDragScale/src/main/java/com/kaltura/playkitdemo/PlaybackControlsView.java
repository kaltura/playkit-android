package com.kaltura.playkitdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerState;

import java.util.Formatter;
import java.util.Locale;

public class PlaybackControlsView extends LinearLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final PKLog log = PKLog.get("PlaybackControlsView");
    private static final int PROGRESS_BAR_MAX = 100;

    private Player player;
    private PlayerState playerState;

    private Formatter formatter;
    private StringBuilder formatBuilder;

    private SeekBar seekBar;
    private TextView tvCurTime, tvTime;
    private ImageButton btnPlay, btnPause, btnFastForward, btnRewind, btnNext, btnPrevious;

    private boolean dragging = false;

    private Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    public PlaybackControlsView(Context context) {
        this(context, null);
    }

    public PlaybackControlsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlaybackControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.playback_layout, this);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        initPlaybackControls();
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
    }


    private void updateProgress() {
        long duration = C.TIME_UNSET;
        long position = C.POSITION_UNSET;
        long bufferedPosition = 0;
        if(player != null){
            duration = player.getDuration();
            position = player.getCurrentPosition();
            bufferedPosition = player.getBufferedPosition();
        }

        if(duration != C.TIME_UNSET){
            log.d("updateProgress Set Duration:" + duration);
            tvTime.setText(stringForTime(duration));
        }

        if (!dragging && position != C.POSITION_UNSET && duration != C.TIME_UNSET) {
            log.d("updateProgress Set Position:" + position);
            tvCurTime.setText(stringForTime(position));
            seekBar.setProgress(progressBarValue(position));
        }

        seekBar.setSecondaryProgress(progressBarValue(bufferedPosition));
        // Remove scheduled updates.
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        if (playerState != PlayerState.IDLE) {
            long delayMs = 1000;
            postDelayed(updateProgressAction, delayMs);
        }
    }

    private int progressBarValue(long position) {
        int progressValue = 0;
        if(player != null){
            long duration = player.getDuration();
            if (duration > 0) {
                progressValue = (int) ((position * PROGRESS_BAR_MAX) / duration);
            }
        }

        return progressValue;
    }

    private long positionValue(int progress) {
        long positionValue = 0;
        if(player != null){
            long duration = player.getDuration();
            positionValue = (duration * progress) / PROGRESS_BAR_MAX;
        }

        return positionValue;
    }

    private String stringForTime(long timeMs) {

        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
        updateProgress();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                if(player != null) {
                    player.play();
                }
                break;
            case R.id.pause:
                player.pause();
                break;
            case R.id.ffwd:
                //Do nothing for now
                break;
            case R.id.rew:
                ///Do nothing for now
                break;
            case R.id.next:
                //Do nothing for now
                break;
            case R.id.prev:
                //Do nothing for now
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            tvCurTime.setText(stringForTime(positionValue(progress)));
        }
    }


    public void onStartTrackingTouch(SeekBar seekBar) {
        dragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        dragging = false;
        player.seekTo(positionValue(seekBar.getProgress()));
    }

    public void release() {
        removeCallbacks(updateProgressAction);
    }

    public void resume() {
        updateProgress();
    }
}
