package com.kaltura.magikapp.magikapp;

import android.app.Activity;

import com.kaltura.magikapp.PlayerControlsView;
import com.kaltura.magikapp.R;


/**
 * Created by itanbarpeled on 19/12/2016.
 */

public class PresenterController implements PresenterControllerInterface, SubPresenterControllerInterface {


    private PresenterView mPresenterView;
    //private String mStandalonePlayerUri;
    private OnPresenterControllerEventListener mOnPresenterControllerListener;
    private LocalPlayerPresenterController mLocalPlayerPresenterController;



    public PresenterController(PresenterView presenterView, OnPresenterControllerEventListener onPresenterControllerListener,
                               Activity activity) {


        mPresenterView = presenterView;
        mOnPresenterControllerListener = onPresenterControllerListener;


        LocalPlayerPresenterView localPlayerPresenterView = (LocalPlayerPresenterView) presenterView.findViewById(R.id.local_player_presenter);

        mLocalPlayerPresenterController = new LocalPlayerPresenterController(localPlayerPresenterView, mOnPresenterControllerListener, activity);

    }




    @Override
    public void setMedia(PlaybackLocation playbackLocation, String standalonePlayerUri) {

        //XXX
        /*
        1. release previous current presenter resources if exist
        2. release previous presenter resources
        3. work with array of controllers
        */

        mLocalPlayerPresenterController.set(standalonePlayerUri);


        //XXX start position 0 isn't relevant for local player in play + connect
        activatePresenter(playbackLocation, 0);

    }



    private void activatePresenter(PlaybackLocation playbackLocation, long playbackStartPosition) {

//       long switch (playbackLocation) {
//
//            case LOCAL_PLAYBACK:
//                mLocalPlayerPresenterController.activate(playbackStartPosition);
//                break;
//
//
//            case REMOTE_PLAYBACK_CONNECTING:
//            case REMOTE_PLAYBACK_CONNECTED:
//                mLocalPlayerPresenterController.inactivate();
//                break;
//
//        }

    }


    public void onApplicationResumed() {
        onApplicationResumed(false);
    }


    public void onApplicationResumed(boolean resumePlaying) {

        //XXX work with array of controllers
        mLocalPlayerPresenterController.onApplicationResumed(resumePlaying);

    }



    public void onApplicationPaused() {

        //XXX work with array of controllers
        mLocalPlayerPresenterController.onApplicationPaused();

    }




    @Override
    public void destroyPresenter() {

        //XXX
        // 1. work with array of controllers
        // 2. combine with setMedia() destroy logic
        mLocalPlayerPresenterController.destroyPlayer();

    }



    public void handleScreenOrientationChange(boolean setFullScreen) {

        //XXX work with array of controllers
        mLocalPlayerPresenterController.handleScreenOrientationChange(setFullScreen);

    }


    @Override
    public void requestActivatePresenter(PresentersType presenterToActivate) {
    }


    public interface OnPresenterControllerEventListener {

        void onPlayerControlsEvent(PlayerControlsView.PlayerControlsEvents.ControlsEvent controlsEvent);

        //XXX
        void onCastEvent();

        void logEvent(String log);

        void handleError(int error);

    }


}
