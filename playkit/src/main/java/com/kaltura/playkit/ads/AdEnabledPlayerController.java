package com.kaltura.playkit.ads;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdsProvider;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator implements AdController, PKAdEventListener {

    private static final PKLog log = PKLog.get("AdEnablController");

    private AdsProvider adsProvider;
    private boolean isPlayerPrepared;
    PlayerConfig.Media mediaConfig;

    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    @Override
    public void prepare(@NonNull final PlayerConfig.Media mediaConfig) {
        this.mediaConfig = mediaConfig;

        isPlayerPrepared = false;
        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                super.prepare(mediaConfig);
                isPlayerPrepared = true;
            } else {
                adsProvider.setAdLoadedListener(this);
            }
        }
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            long adDuration = adsProvider.getDuration();
            log.v("getDuration: " + adDuration);
           return 1000 * adDuration;
        } else {
            return super.getDuration();
        }
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            long adPosition = adsProvider.getCurrentPosition();
            log.v("getCurrentPosition = " + adPosition);
            return 1000 * adPosition;
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
    public void onEvent(PKEvent event) {
        Enum receivedEventType = event.eventType();

        if (!isPlayerPrepared && (receivedEventType == AdEvent.Type.CONTENT_RESUME_REQUESTED || receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED)) {
            prepare(mediaConfig);
            if (adsProvider != null) {
                adsProvider.removeAdLoadedListener();
            }
            isPlayerPrepared = true;
            super.play();
        }
    }
}
