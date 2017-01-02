package com.kaltura.magikapp;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.BACK_BUTTON;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.DRAGGING;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.DRAGG_ENDED;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.DRAGG_STARTED;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.FULL_SCREEN_SIZE;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.PLAY_PAUSE;
import static com.kaltura.magikapp.PlayerControlsView.PlayerControlsEvents.ControlsEvent.ButtonClickEvent.SELECT_TRACKS_DIALOG;


/**
 * Created by itanbarpeled on 26/11/2016.
 */

public class PlayerControlsView extends FrameLayout {


    private ImageView mPlayPause;
    private ImageView mSettings;
    private ImageView mBackIcon;
    private ImageView mScreenSizeIcon;
    private ProgressBar mProgressBar;
    private TextView mTimeIndicator;
    private SeekBar mSeekBar;
    private ConstraintLayout mPlayerControls;

    private PlayerControlsEvents mControlsClickListener;


    public PlayerControlsView(Context context) {
        this(context, null);
    }

    public PlayerControlsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public PlayerControlsView(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.player_controls_layout, this);

        initPlayerControls();
        setPlayerControlsListeners();

    }


    private void initPlayerControls() {

        mProgressBar = (ProgressBar) findViewById(R.id.icon_progress_bar);
        mTimeIndicator = (TextView) findViewById(R.id.time_indicator);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mPlayPause = (ImageView) findViewById(R.id.icon_play_pause);
        mSettings = (ImageView) findViewById(R.id.icon_settings);
        mBackIcon = (ImageView) findViewById(R.id.icon_back);
        mScreenSizeIcon = (ImageView) findViewById(R.id.icon_screen_size);
        mPlayerControls = (ConstraintLayout) findViewById(R.id.player_controls);
    }

    public boolean getControlsVisibility() {
        return mPlayerControls.getVisibility() == VISIBLE;
    }


    public void setControlsVisibility(boolean toShow) {
        mPlayerControls.setVisibility(toShow ? VISIBLE : INVISIBLE);
    }


    public void setProgressBarVisibility(boolean toShow) {
        mProgressBar.setVisibility(toShow ? VISIBLE : INVISIBLE);
    }

    public void setBackButtonVisibility(boolean toShow) {
        mBackIcon.setVisibility(toShow ? VISIBLE : INVISIBLE);
    }

    public void setScreenSizeButtonVisibility(boolean toShow) {
        mScreenSizeIcon.setVisibility(toShow ? VISIBLE : GONE);
    }

    public void setSeekBarVisibility(boolean toShow) {
        mSeekBar.setVisibility(toShow ? VISIBLE : GONE);
    }

    public void setSeekBarProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    public void setSeekBarMode(boolean isEnabled) {
        mSeekBar.setEnabled(isEnabled);
    }

    public void setSeekBarSecondaryProgress(int secondaryProgress) {
        mSeekBar.setSecondaryProgress(secondaryProgress);
    }


    public void setPlayPauseVisibility(boolean toShow, boolean toShowPlayView, boolean isEnded) {

        if (toShow) {
            if (toShowPlayView) {
                mPlayPause.setImageResource(R.mipmap.play);
            } else {
                mPlayPause.setImageResource(R.mipmap.pause);
            }
            mPlayPause.setVisibility(View.VISIBLE);

        } else {
            mPlayPause.setVisibility(View.INVISIBLE);
        }
    }

    public void setPlayPauseVisibility(boolean toShow, boolean toShowPlayView) {
        setPlayPauseVisibility(toShow, toShowPlayView, false);
    }


    public void setControlsClickListener(PlayerControlsEvents controlsClickListener) {
        mControlsClickListener = controlsClickListener;
    }


    public void setTimeIndicator(String timeIndicator) {
        mTimeIndicator.setText(timeIndicator);
    }


    private void setPlayerControlsListeners() {

        mPlayPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(PLAY_PAUSE));
            }
        });

        mSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(SELECT_TRACKS_DIALOG));
            }
        });


        mBackIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(BACK_BUTTON));
            }
        });

        mScreenSizeIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(FULL_SCREEN_SIZE));
            }
        });


        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(DRAGG_STARTED));
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(DRAGGING, progress));
                }
            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mControlsClickListener.onControlsEvent(new PlayerControlsEvents.ControlsEvent(DRAGG_ENDED, seekBar.getProgress()));
            }
        });

    }


    public interface PlayerControlsEvents {

        class ControlsEvent {

            public enum ButtonClickEvent {
                SELECT_TRACKS_DIALOG, BACK_BUTTON,
                FULL_SCREEN_SIZE, PLAY_PAUSE, DRAGG_STARTED, DRAGGING, DRAGG_ENDED
            }

            private int mPosition;
            private ButtonClickEvent mButtonClickEvent;

            ControlsEvent(ButtonClickEvent buttonClickEvent) {
                mButtonClickEvent = buttonClickEvent;
            }

            ControlsEvent(ButtonClickEvent buttonClickEvent, int position) {
                mButtonClickEvent = buttonClickEvent;
                mPosition = position;
            }


            public ButtonClickEvent getButtonClickEvent() {
                return mButtonClickEvent;
            }

            public int getPosition() {
                return mPosition;
            }
        }


        void onControlsEvent(ControlsEvent controlsEvent);
    }

    //TODO Itan check if needed after Gilad pull request!
    public void toggleControlsVisibility(boolean doShow) {
        if (doShow) {
            mPlayerControls.setVisibility(VISIBLE);
            mPlayPause.setVisibility(VISIBLE);
        } else {
            mPlayerControls.setVisibility(INVISIBLE);
            mPlayPause.setVisibility(INVISIBLE);
        }
    }

}
