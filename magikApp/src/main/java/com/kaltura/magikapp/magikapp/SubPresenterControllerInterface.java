package com.kaltura.magikapp.magikapp;

/**
 * Created by itanbarpeled on 24/12/2016.
 */

public interface SubPresenterControllerInterface {

    enum PresentersType {

        LOCAL_PLAYER_PRESENTER,
        CAST_PRESENTER

    }


    void requestActivatePresenter(PresentersType presenterToActivate);

}
