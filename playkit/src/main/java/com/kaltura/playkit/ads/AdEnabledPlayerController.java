package com.kaltura.playkit.ads;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.plugins.ads.PKPrepareReason;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator implements AdController, PKAdProviderListener{

    private static final PKLog log = PKLog.get("AdEnablController");

    private AdsProvider adsProvider;
    private boolean isPlayerPrepared;
    private PlayerConfig.Media mediaConfig;

    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    /*
     In order to avoid network resources race between IMA and Content CDN
     we prevent the prepare until AD is STARTED, No Pre-Roll or AD ERROR is received
    */
    @Override
    public void prepare(@NonNull final PlayerConfig.Media mediaConfig) {
        this.mediaConfig = mediaConfig;

        isPlayerPrepared = false;
        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                super.prepare(mediaConfig);
                isPlayerPrepared = true;
            } else {
                adsProvider.setAdProviderListener(this);
            }
        }
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
                //super.getView().hideVideoSurface();
                adsProvider.init();
                return;
            } else if (adsProvider.isAdDisplayed()) {
                adsProvider.resume();
                return;
            }
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
    public AdController getAdController() {
        return this;
    }

    @Override
    public void onAdLoadingFinished(PKPrepareReason pkPrepareReason) {
        log.d("onAdLoadingFinished pkPrepareReason = " + pkPrepareReason.name());
        if (pkPrepareReason == PKPrepareReason.UNKNOWN) {
            return;
        }
        prepare(mediaConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
        isPlayerPrepared = true;
    }
}
