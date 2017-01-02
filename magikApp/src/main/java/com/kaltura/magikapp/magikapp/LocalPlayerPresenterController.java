package com.kaltura.magikapp.magikapp;

import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import com.kaltura.magikapp.PlayerControlsView;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.data.JsonFetchHandler;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;


/**
 * Created by itanbarpeled on 21/12/2016.
 */

public class LocalPlayerPresenterController {


    private static final String INIT_LOG = null;


    PresenterController.OnPresenterControllerEventListener mOnPresenterControllerListener;
    private LocalPlayerPresenterView mLocalPlayerPresenterView;
    private PlayerControlsController mPlayerControlsController;
    private LinearLayout mPlayerView;
    private Activity mActivity;
    private PlayerConfig mPlayerConfig;
    private Player mPlayer;
    private boolean mIsPresenterActivated;


    public LocalPlayerPresenterController(LocalPlayerPresenterView localPlayerPresenterView,
                                          PresenterController.OnPresenterControllerEventListener onPresenterControllerListener, Activity activity) {

        mLocalPlayerPresenterView = localPlayerPresenterView;
        mOnPresenterControllerListener = onPresenterControllerListener;
        mIsPresenterActivated = false;
        mActivity = activity;

        setPlayerControls();

    }


    private void setPlayerControls() {

        mPlayerView = (LinearLayout) mLocalPlayerPresenterView.findViewById(R.id.player_view);
        PlayerControlsView playerControlsView = (PlayerControlsView) mLocalPlayerPresenterView.findViewById(R.id.player_controls_view);

        mPlayerControlsController = new PlayerControlsController(playerControlsView, mOnPresenterControllerListener);


        mLocalPlayerPresenterView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mPlayerControlsController.handleContainerClick();
            }

        });

    }



    public void set(String standalonePlayerUri) {

        destroyPlayer();
        fetchPlayerConfig(standalonePlayerUri);

    }


    public void activate(long playbackStartPosition) {

        mIsPresenterActivated = true;
        mLocalPlayerPresenterView.setVisibility(View.VISIBLE);


        if (isResumingCastPosition()) {
            resumePlayingFromCastPosition(playbackStartPosition);
        }

    }


    public void inactivate() {

        mIsPresenterActivated = false;
        mLocalPlayerPresenterView.setVisibility(View.INVISIBLE);

        if (mPlayer != null) {
            mPlayer.pause();
        }

    }



    public void onApplicationResumed(boolean resumePlaying) {

        if (mPlayer != null) {

            mPlayer.onApplicationResumed();
            mPlayerControlsController.onApplicationResumed();

            if (resumePlaying) {
                mPlayer.play();
            }
        }

    }



    public void onApplicationPaused() {

        if (mPlayer != null) {
            mPlayerControlsController.onApplicationPaused();
            mPlayer.onApplicationPaused();
        }

    }


    public long getCurrentPlaybackPosition() {

        return mPlayer != null ? mPlayer.getCurrentPosition() : 0;

    }


    public void handleScreenOrientationChange(boolean setFullScreen) {

        mPlayerControlsController.handleScreenOrientationChange(setFullScreen);

    }


    public void destroyPlayer() {

        if (mPlayerControlsController != null) {
            mPlayerControlsController.destroyPlayer();
        }

        if (mPlayer != null) {
            mPlayer.destroy();
            mPlayer = null;
            mPlayerConfig = null;
        }


        mOnPresenterControllerListener.logEvent(INIT_LOG);

    }


    //XXX - decide + comment
    private boolean isResumingCastPosition() {
        return mPlayer != null;
    }


    private void resumePlayingFromCastPosition(long playbackStartPosition) {

        long playbackResumePosition = Math.min(playbackStartPosition, mPlayer.getDuration());

        mPlayer.seekTo(playbackResumePosition);
        mPlayer.play();

    }


    private void fetchPlayerConfig(String standalonePlayerUri) {
        JsonFetchHandler.fetchPlayerConfig(standalonePlayerUri, mActivity.getApplicationContext(), new JsonFetchHandler.OnJsonFetchedListener() {

            @Override
            public void onJsonFetched(String playerConfigJson) {

                if (TextUtils.isEmpty(playerConfigJson)) {
                    mOnPresenterControllerListener.handleError(R.string.something_went_wrong);
                    return;
                }

                createPlayer(playerConfigJson);

            }

        });
    }



    private void createPlayer(String playerConfigJson) {

        PlayerProvider playerProvider = new PlayerProvider();
        playerProvider.getPlayer(playerConfigJson, mActivity, new PlayerProvider.OnPlayerReadyListener() {


            @Override
            public void onPlayerReady(Player player, PlayerConfig playerConfig) {

                if (player == null) {
                    mOnPresenterControllerListener.handleError(R.string.something_went_wrong);
                    return;
                }

                mPlayer = player;
                mPlayerConfig = playerConfig;
                prepareToPlay();
            }
        });

    }



    private void prepareToPlay() {

        mPlayerView.removeAllViews();
        mPlayerView.addView(mPlayer.getView());
        mPlayerControlsController.setPlayer(mPlayer, mPlayerConfig);

        //XXX - decide + comment
        /*
        by default we pause the player, regardless autoPlay true or false.
        when activate() method will be called - we will start the player if autoPlay == true
         */
        if (!mIsPresenterActivated) {
            mPlayer.pause();
        }


        setLogListener();

    }


    private void setLogListener() {

        final Handler logHandler = new Handler();


        mPlayer.addEventListener(new PKEvent.Listener() {

            @Override
            public void onEvent(final PKEvent event) {

                logHandler.post(new Runnable() {

                    @Override
                    public void run() {

                        String log = ((LogEvent) event).log;

                        if (mOnPresenterControllerListener != null) {
                            mOnPresenterControllerListener.logEvent(log);
                        }

                    }

                });
            }
        }, LogEvent.LogType.LogEvent);


    }





}
