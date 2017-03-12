package com.kaltura.playkit.ads;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator implements AdController {

    private static final PKLog log = PKLog.get("AdEnablController");

    AdsProvider adsProvider;
    PlayerConfig.Media mediaConfig;
    boolean isLoaded = false;
    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            long adDuration = adsProvider.getDuration();
            log.v("getDuration: " + adDuration);
            return Consts.MILLISECONDS_MULTIPLIER * adDuration;
        } else {
            return super.getDuration();
        }
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            long adPosition = adsProvider.getCurrentPosition();
            log.v("getCurrentPosition = " + adPosition);
            return Consts.MILLISECONDS_MULTIPLIER * adPosition;
        } else {
            return super.getCurrentPosition();
        }
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo is not enabled during AD playback");
            return;
        } else {
            super.seekTo(position);
        }
    }

    @Override
    public void play() {
        log.d("PLAY isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
        if (adsProvider != null) {
            if (!adsProvider.isAdRequested()) {
                super.getView().hideVideoSurface();
                adsProvider.start();
                return;
            } else if (adsProvider.isAdDisplayed()) {
                adsProvider.resume();
                return;
            }
        }
        getView().showVideoSurface();
        if (!this.isLoaded){
            this.loadMediaWhileAdIsPlaying();
        }
        super.play();

    }

    @Override
    public void pause() {
        log.d("PAUSE isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
        if (adsProvider.isAdDisplayed()) {
            adsProvider.pause();
        } else {
            super.pause();
        }
    }

    @Override
    public void skipAd() {
        adsProvider.skipAd();
    }


    @Override
    public void loadMediaWhileAdIsPlaying(){
        this.isLoaded = true;
        super.load();
    }

    @Override
    public void prepare(PlayerConfig.Media mediaConfig){
        super.selectSource(mediaConfig);
        super.selectPlayer();
     }

    @Override
    public AdController getAdController() {
        return this;
    }
}
