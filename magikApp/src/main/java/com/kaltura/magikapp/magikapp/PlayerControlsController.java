package com.kaltura.magikapp.magikapp;

import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.widget.ImageView;

import com.kaltura.magikapp.PlayerControlsView;
import com.kaltura.magikapp.R;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playkit.PKMediaEntry.MediaEntryType.Live;
import static com.kaltura.playkit.PlayerEvent.Type.CAN_PLAY;
import static com.kaltura.playkit.PlayerEvent.Type.ENDED;
import static com.kaltura.playkit.PlayerEvent.Type.PAUSE;
import static com.kaltura.playkit.PlayerEvent.Type.PLAYING;
import static com.kaltura.playkit.PlayerEvent.Type.SEEKING;
import static com.kaltura.playkit.PlayerEvent.Type.TRACKS_AVAILABLE;

/**
 * Created by itanbarpeled on 26/11/2016.
 */

public class PlayerControlsController implements PlayerControlsControllerInterface, PlayerControlsView.PlayerControlsEvents {

    private static final PKLog log = PKLog.get("PlayerControls");

    private static final int UPDATE_TIME_INTERVAL = 300;
    private static final int PROGRESS_BAR_MAX = 100;
    private static final int REMOVE_CONTROLS_TIMEOUT = 3250;

    private static final int FIFTEEN_MIN = 15 * 60 * 1000;
    private static final int FIFTEEN_SEC = 15 * 1000;

    private Player mPlayer;
    private PlayerConfig mPlayerConfig;
    private Enum mPlayerState;
    private TracksController mTracksController;
    private PlayerControlsView mPlayerControlsView;

    private PresenterController.OnPresenterControllerEventListener mOnPresenterControllerListener;

    private Timer mTimer;
    private UpdateProgressTask mUpdateProgressTask;
    private Formatter formatter;
    private StringBuilder formatBuilder;

    private boolean mIsDragging;
    private boolean isAdDisplayed;
    private List<Long> adCuePoints;
    private AdInfo adInfo;
    private boolean allAdsCompeted;


    public PlayerControlsController(PlayerControlsView controlsView, PresenterController.OnPresenterControllerEventListener onPresenterControllerListener) {

        mPlayerControlsView = controlsView;
        mOnPresenterControllerListener = onPresenterControllerListener;

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        mIsDragging = false;

        mPlayerControlsView.setControlsClickListener(this);
        setControlsView(false);

    }



    private void setUpdateProgressTask(boolean startTracking) {

        if (startTracking) {

            if (mPlayer == null) { // don't start timer before player is ready
                return;
            }

            if (mTimer == null) { // init only once
                mTimer = new Timer();
                mUpdateProgressTask = new UpdateProgressTask();
                mTimer.schedule(mUpdateProgressTask, 0, UPDATE_TIME_INTERVAL);
            }


        } else { // pause timer

            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
                mUpdateProgressTask = null;
            }
        }
    }



    @Override
    public void setPlayer(Player player, PlayerConfig playerConfig) {
        mPlayer = player;
        mPlayerConfig = playerConfig;

        mPlayerState = null; // init player state
        setPlayerListeners();
        initTracksController();
    }



    private void initTracksController() {

        if (mTracksController == null) { // TracksController should be created only once
            mTracksController = new TracksController(mPlayerControlsView);
        }

        mTracksController.setPlayer(mPlayer);

    }



    @Override
    public void handleContainerClick() {

        if (mPlayerState == null) {
            return;
        }

        boolean isControlsVisible = mPlayerControlsView.getControlsVisibility();
        log.v("handleContainerClick mPlayerState = " + mPlayerState + " isControlsVisible = " + isControlsVisible);
        // toggle visibility

        mPlayerControlsView.setControlsVisibility(!isControlsVisible);
        if (adInfo != null) {
            if (adInfo.getAdPodPosition() < adInfo.getAdPodCount()) {
                mPlayerControlsView.setSeekBarMode(false);
            }
        }
        if (isLiveAndNoDVR()) {
            mPlayerControlsView.setSeekBarVisibility(false);
        } else {
            mPlayerControlsView.setSeekBarVisibility(true);
        }
        if (isControlsVisible) {
            if (mPlayerState != SEEKING) {
                mPlayerControlsView.setPlayPauseVisibility(false, false);
            }
        } else {
            // show pause button if currently playing
            mPlayerControlsView.setPlayPauseVisibility(true, !(mPlayerState == PLAYING || mPlayerState == AdEvent.Type.STARTED || mPlayerState == AdEvent.Type.RESUMED));

        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPlayerState == PLAYING || mPlayerState == AdEvent.Type.STARTED || mPlayerState == AdEvent.Type.RESUMED) {
                    mPlayerControlsView.setControlsVisibility(false);
                    mPlayerControlsView.setPlayPauseVisibility(false, false);
                }
            }
        }, REMOVE_CONTROLS_TIMEOUT);
    }

    private boolean isLiveAndNoDVR() {
        return mPlayerConfig != null &&  mPlayerConfig.media.getMediaEntry() != null &&
                mPlayerConfig.media.getMediaEntry().getMediaType() != null &&
                mPlayerConfig.media.getMediaEntry().getMediaType().equals(Live) &&
                mPlayer != null && mPlayer.getDuration() < FIFTEEN_MIN;
    }

    private boolean isLiveAndDVR() {

        return mPlayerConfig != null && mPlayerConfig.media.getMediaEntry() != null &&
               mPlayerConfig.media.getMediaEntry().getMediaType() != null &&
               mPlayerConfig.media.getMediaEntry().getMediaType().equals(Live) &&
               mPlayer != null && mPlayer.getDuration() >= FIFTEEN_MIN;
    }

    @Override
    public void handleScreenOrientationChange(boolean setFullScreen) {

        mPlayerControlsView.setBackButtonVisibility(setFullScreen);
        mPlayerControlsView.setScreenSizeButtonVisibility(!setFullScreen);

    }


    private void updateProgress() {

        long duration = 0;
        long position = 0;
        long bufferedPosition = 0;

        if (mPlayer != null) {
            duration = (mPlayer.getDuration() > 0) ? mPlayer.getDuration() : duration;
            position = mPlayer.getCurrentPosition();
            bufferedPosition = mPlayer.getBufferedPosition();
        }


        if (!mIsDragging) {
            setTimeIndicator(position, duration);
        }

        if (!mIsDragging) {
            mPlayerControlsView.setSeekBarProgress(progressBarValue(duration, position));
        }

        mPlayerControlsView.setSeekBarSecondaryProgress(progressBarValue(duration, bufferedPosition));

    }

    private void setTimeIndicator(long position, long duration) {

        String timeSeparator;
        StringBuilder timeIndicator = new StringBuilder();
        timeSeparator =  mPlayerControlsView.getContext().getString(R.string.time_indicator_separator);

        if (mPlayerConfig != null && mPlayerConfig.media.getMediaEntry().getMediaType().equals(Live)) {
            long distanceFromLive = duration - position;
            log.d("distanceFromLive = " + distanceFromLive);
            if (isLiveAndNoDVR() || distanceFromLive <= FIFTEEN_SEC) {
                timeIndicator.append("Live");
            } else if (isLiveAndDVR()){
                timeIndicator.append(" -" + stringForTime(distanceFromLive) + "  Live");
            }
        } else {
            timeIndicator.append(stringForTime(position));
            timeIndicator.append(timeSeparator);
            timeIndicator.append(stringForTime(duration));
        }

        mPlayerControlsView.setTimeIndicator(timeIndicator.toString());

    }


    private int progressBarValue(long duration, long position) {

        if (duration > 0) {
            return (int) ((position * PROGRESS_BAR_MAX) / duration);
        } else {
            return 0;
        }

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


    private long positionValue(long progress) {
        long positionValue = 0;

        if (mPlayer != null) {
            long duration = mPlayer.getDuration();
            positionValue = (duration * progress) / PROGRESS_BAR_MAX;
        }

        return positionValue;
    }

    @Override
    public void onApplicationResumed() {
        setUpdateProgressTask(true);
        if (isAdDisplayed) {
            showControlsWithPlay();
        } else {
            mPlayerControlsView.setSeekBarMode(true);
            if (!isPlaybackEndedState()) {
                showControlsWithPlay();
            }
        }

    }

    private boolean isPlaybackEndedState() {
        return mPlayerState == ENDED || (allAdsCompeted && isPostrollAvailableInAdCuePoint());
    }

    @Override
    public void onApplicationPaused() {
        setUpdateProgressTask(false);
        if (isAdDisplayed) {
            hideControls();
        }
    }


    public void destroyPlayer() {

        setControlsView(false);
        setUpdateProgressTask(false);

        if (mTracksController != null) {
            mTracksController.destroyController();
        }


    }


    private void togglePlayPauseReplay() {
        log.v("togglePlayPause mPlayerState = " + mPlayerState);
        if (isPlaybackEndedState()) {
            hideControls();
            mPlayer.seekTo(0);
            mPlayer.play();
            cleanAdData();
        } else if (mPlayerState == PLAYING || mPlayerState == AdEvent.Type.STARTED || mPlayerState == AdEvent.Type.RESUMED) {

            setControlsView(true);
            mPlayer.pause();
        } else if (mPlayerState == CAN_PLAY || mPlayerState == PAUSE || mPlayerState == AdEvent.Type.PAUSED) {
            setControlsView(false);
            mPlayer.play();

        }
    }

    private void cleanAdData() {
        allAdsCompeted = false;
        adCuePoints = null;
        adInfo = null;
    }

    private void setControlsView(boolean showPlayer) {
        log.v("setControlsView showPlayer = " + showPlayer);
        if (showPlayer) {

            mPlayerControlsView.setPlayPauseVisibility(true, true);
            mPlayerControlsView.setProgressBarVisibility(false);

        } else {

            mPlayerControlsView.setControlsVisibility(false);
            mPlayerControlsView.setPlayPauseVisibility(false, false);
        }

    }

    private void setPlayerListeners() {

        mPlayer.addEventListener(new PKEvent.Listener() {

            @Override
            public void onEvent(PKEvent event) {
                log.v("addEventListener " + event.eventType());

                Enum receivedEventType = event.eventType();

                if (receivedEventType == CAN_PLAY) {
                    if (!mPlayer.isPlaying()) {
                        setControlsView(true);
                    }
                    setUpdateProgressTask(true);
                } else if (receivedEventType == TRACKS_AVAILABLE) {
                    if (isLiveAndDVR()) {
                        mPlayer.seekTo(mPlayer.getDuration());
                    }
                } else if (receivedEventType == PLAYING) {
                    if (mPlayer.getCurrentPosition() >= mPlayer.getDuration()) {
                        showControlsWithPlay();
                    } else {
                        setControlsView(false);
                    }
                } else if (receivedEventType == ENDED) {
                    if (!isPostrollAvailableInAdCuePoint()) {
                        showControlsWithReplay();
                    }
                } else if (receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED) {
                    adCuePoints =  ((AdEvent.AdCuePointsUpdateEvent)event).cuePoints;

                } else if (receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED) {
                    isAdDisplayed = false;
                    allAdsCompeted = true;
                    if (mPlayer.getCurrentPosition() >= mPlayer.getDuration()) {
                        if (isPostrollAvailableInAdCuePoint()) {
                            showControlsWithReplay();
                        }
                    }
                } else if (receivedEventType == AdEvent.Type.CONTENT_PAUSE_REQUESTED) {
                    mPlayerControlsView.setProgressBarVisibility(true);
                    setControlsView(false);
                } else if (receivedEventType == AdEvent.Type.STARTED) {
                    adInfo = ((AdEvent.AdStartedEvent)event).adInfo;

                    isAdDisplayed = true;
                    allAdsCompeted = false;
                    mPlayerControlsView.setProgressBarVisibility(false);
                    mPlayerControlsView.setSeekBarMode(false);
                    setControlsView(false);
                } else if (receivedEventType == AdEvent.Type.TAPPED) {
                    handleContainerClick();
                } else if (receivedEventType == AdEvent.Type.COMPLETED) {
                    mPlayerControlsView.setSeekBarMode(true);
                    isAdDisplayed = false;
                    if (adInfo != null) {
                        if (adInfo.getAdPodPosition() ==  adInfo.getAdPodCount()) {
                            adInfo = null;
                        }
                    }
                }  else if (receivedEventType == AdEvent.Type.SKIPPED) {
                    mPlayerControlsView.setSeekBarMode(true);
                }

                if (event instanceof PlayerEvent || receivedEventType == AdEvent.Type.PAUSED || receivedEventType == AdEvent.Type.RESUMED ||
                        receivedEventType == AdEvent.Type.STARTED || (receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED && isPostrollAvailableInAdCuePoint())) {
                    mPlayerState = event.eventType();
                }
            }
        }, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, CAN_PLAY, PlayerEvent.Type.SEEKING, PlayerEvent.Type.SEEKED, PlayerEvent.Type.PLAYING,  PlayerEvent.Type.ENDED, PlayerEvent.Type.TRACKS_AVAILABLE,

           AdEvent.Type.LOADED, AdEvent.Type.SKIPPED, AdEvent.Type.TAPPED, AdEvent.Type.CONTENT_PAUSE_REQUESTED, AdEvent.Type.CONTENT_RESUME_REQUESTED, AdEvent.Type.STARTED, AdEvent.Type.PAUSED, AdEvent.Type.RESUMED,
           AdEvent.Type.COMPLETED, AdEvent.Type.ALL_ADS_COMPLETED, AdEvent.Type.CUEPOINTS_CHANGED);


        mPlayer.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

                PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;
                log.v("addStateChangeListener " + event.eventType() + " = " + stateChanged.newState);
                switch (stateChanged.newState){
                    case IDLE:
                        log.d("StateChange Idle");
                        //TEMP untill no IDLE fired when not needed
                        mPlayerControlsView.setProgressBarVisibility(false);
                        break;
                    case LOADING:
                        log.d("StateChange Loading");
                        break;
                    case READY:
                        log.d("StateChange Ready");
                        mPlayerControlsView.setProgressBarVisibility(false);
                        break;
                    case BUFFERING:
                        log.e("StateChange Buffering");
                        mPlayerControlsView.setProgressBarVisibility(true);
                        break;
                }

            }
        });

    }


    @Override
    public void onControlsEvent(ControlsEvent controlsEvent) {

        ControlsEvent.ButtonClickEvent buttonClickEvent = controlsEvent.getButtonClickEvent();

        switch (buttonClickEvent) {

            case SELECT_TRACKS_DIALOG:
                mPlayerControlsView.toggleControlsVisibility(false);
                mTracksController.toggleTrackSelectionDialogVisibility(true);
                break;

            case BACK_BUTTON:
                handleScreenSizeChange(controlsEvent);
                break;

            case FULL_SCREEN_SIZE:
                handleScreenSizeChange(controlsEvent);
                break;

            case PLAY_PAUSE:
                togglePlayPauseReplay();
                break;

            case DRAGG_STARTED:
            case DRAGGING:
            case DRAGG_ENDED:
                handleScrubBarDragging(buttonClickEvent, controlsEvent.getPosition());
                break;
        }
    }

    private void handleScreenSizeChange(ControlsEvent controlsEvent) {
        mOnPresenterControllerListener.onPlayerControlsEvent(controlsEvent);
    }


    private void handleScrubBarDragging(ControlsEvent.ButtonClickEvent buttonClickEvent, long position) {

        switch (buttonClickEvent) {

            case DRAGG_STARTED:
                mIsDragging = true;
                break;

            case DRAGGING:
                setTimeIndicator(positionValue(position), mPlayer.getDuration());
                break;

            case DRAGG_ENDED:
                mIsDragging = false;
                seek(positionValue(position));
                break;
        }
    }

    private void seek(long position) {
        if (mPlayer != null) {
            mPlayer.seekTo(position);
        }
    }

    private void showMessage(int string) {

        if (mPlayerControlsView != null) {
            ImageView itemView = (ImageView) mPlayerControlsView.findViewById(R.id.icon_play_pause);
            Snackbar snackbar = Snackbar.make(itemView, string, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    private void showControlsWithPlay() {
        log.v("showControlsWithPlay");
        mPlayerControlsView.setPlayPauseVisibility(true, true);
        mPlayerControlsView.setProgressBarVisibility(false);
        mPlayerControlsView.setControlsVisibility(true);
    }

    private void showControlsWithPause() {
        log.v("showControlsWithPause");
        mPlayerControlsView.setPlayPauseVisibility(true, true);
        mPlayerControlsView.setProgressBarVisibility(false);
        mPlayerControlsView.setControlsVisibility(true);

    }

    private void showControlsWithReplay() {
        log.v("showControlsWithPause");
        mPlayerControlsView.setPlayPauseVisibility(true, true, true);
        mPlayerControlsView.setProgressBarVisibility(false);
        mPlayerControlsView.setControlsVisibility(true);
    }

    private void hideControls() {
        log.v("hideControls");
        mPlayerControlsView.setControlsVisibility(false);
        mPlayerControlsView.setPlayPauseVisibility(false, false);
        mPlayerControlsView.setControlsVisibility(false);

    }
    private class UpdateProgressTask extends TimerTask {

        @Override
        public void run() {

            if (mPlayerControlsView != null) {

                mPlayerControlsView.post(new Runnable() {

                    @Override
                    public void run() {
                        updateProgress();
                    }
                });
            }
        }
    }


    private boolean isPostrollAvailableInAdCuePoint() {
       if (adCuePoints != null && adCuePoints.size() > 0) {
           if (adCuePoints.get(adCuePoints.size() -1) < 0) {
               return true;
           }
       }
       return false;
    }

    //XXX - what is this interface
    public interface OnPlayerControlsEventListener {
        void onPlayerControlsEvent(ControlsEvent controlsEvent);
    }


}
