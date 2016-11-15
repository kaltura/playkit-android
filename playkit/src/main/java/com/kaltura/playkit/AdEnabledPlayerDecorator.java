package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 15/11/2016.
 */

public class AdEnabledPlayerDecorator extends PlayerDecorator {
    public AdEnabledPlayerDecorator(AdProvider adProvider) {
        
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
