package com.kaltura.playkit;

import android.content.Context;

import com.kaltura.playkit.player.PlayerController;

/**
 * Created by Noam Tamim @ Kaltura on 15/11/2016.
 */

class AdEnabledPlayerController extends PlayerController {
    AdEnabledPlayerController(Context context, PlayerConfig.Media mediaConfig, AdProvider adProvider) {
        super(context, mediaConfig);
    }

    @Override
    public void play() {
        // TODO: maybe play ad
        super.play();
    }

    @Override
    public void pause() {
        // TODO: maybe pause ad
        super.pause();
    }
}
