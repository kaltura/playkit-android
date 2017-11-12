/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.ads;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.utils.Consts;

/**
 * @hide
 */

public class AdEnabledPlayerController extends PlayerDecorator implements AdController, PKAdProviderListener {

    private static final PKLog log = PKLog.get("AdEnablController");

    private AdsProvider adsProvider;
    private PKMediaConfig mediaConfig;

    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    /*
     In order to avoid network resources race between plugin and Content CDN
     we prevent the prepare until AD is STARTED, No Pre-Roll or AD ERROR is received
    */
    @Override
    public void prepare(@NonNull final PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;

        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                log.d("calling super.prepare");
                super.prepare(mediaConfig);
            } else {
                log.d("setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            long adDuration = adsProvider.getDuration();
            return Consts.MILLISECONDS_MULTIPLIER * adDuration;
        }

        return super.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        boolean isAdDisplayed = adsProvider.isAdDisplayed();
        if (isAdDisplayed) {
            long adPosition = adsProvider.getCurrentPosition();
            return Consts.MILLISECONDS_MULTIPLIER * adPosition;
        }

        return super.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo is not enabled during AD playback");
            return;
        }

        super.seekTo(position);
    }

    @Override
    public void play() {
        log.d("PLAY plugin decorator");
        if (adsProvider != null && !adsProvider.isAdError()) {
            log.d("PLAY plugin decorator isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted = " + adsProvider.isAllAdsCompleted());
            if (!adsProvider.isAllAdsCompleted()) {
                if (!adsProvider.isAdRequested()) {
                    adsProvider.start();
                    return;
                } else if (adsProvider.isAdDisplayed()) {
                    adsProvider.resume();
                    return;
                }
            }
        }
        log.d("decorator Calling player play");
        getView().showVideoSurface();
        super.play();

    }

    @Override
    public void pause() {
        log.d("PAUSE decorator isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted " + adsProvider.isAllAdsCompleted());
        if (adsProvider.isAdDisplayed() && !adsProvider.isAdError()) {
            adsProvider.pause();
        } else {
            log.d("decorator Calling player pause");
            super.pause();
        }
    }

    @Override
    public void stop() {
        log.d("AdEnabled stop");
        if (adsProvider != null) {
            adsProvider.destroyAdsManager();
        }
        super.stop();
    }

    @Override
    public void skipAd() {
        if (adsProvider != null && !adsProvider.isAdError()) {
            adsProvider.skipAd();
        }
    }

    @Override
    public void openLearnMore() {
        if (adsProvider != null && !adsProvider.isAdError()) {
            adsProvider.openLearnMore();
        }
    }

    @Override
    public void openCompanionAdLearnMore() {
        adsProvider.openComapanionAdLearnMore();
    }

    @Override
    public void screenOrientationChanged(boolean isFullScreen) {
        adsProvider.screenOrientationChanged(isFullScreen);
    }

    @Override
    public void volumeKeySilent(boolean isMute) {
        adsProvider.volumeKeySilent(isMute);
    }

    @Override
    public long getAdCurrentPosition() {
        if (adsProvider != null) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getAdDuration() {
        if (adsProvider != null) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getDuration();
        }
        return Consts.TIME_UNSET;
    }

    @Override
    public AdController getAdController() {
        //log.d("AdDecorator getAdController");
        return this;
    }

    @Override
    public void onAdLoadingFinished() {
        log.d("onAdLoadingFinished pkPrepareReason");
        if (mediaConfig == null) {
            log.d("onAdLoadingFinished mediaConfig == null");
            return;
        }
        prepare(mediaConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
    }
}
