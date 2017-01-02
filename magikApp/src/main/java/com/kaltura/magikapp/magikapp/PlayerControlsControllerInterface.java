package com.kaltura.magikapp.magikapp;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;

/**
 * Created by itanbarpeled on 26/11/2016.
 */

public interface PlayerControlsControllerInterface {

    void handleScreenOrientationChange(boolean setFullScreen);

    //XXX
    void onApplicationResumed();

    //XXX
    void onApplicationPaused();

    //XXX
    void setPlayer(Player player, PlayerConfig playerConfig);

    //XXX
    void handleContainerClick();



}
