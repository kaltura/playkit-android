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

import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

/**
 * @hide
 */

public class AdDAIEnabledPlayerController extends AdEnabledPlayerController {

    private static final PKLog log = PKLog.get("AdEnablController");

    private AdsProvider adsProvider;
    private PKMediaConfig mediaConfig;

    public AdDAIEnabledPlayerController(AdsProvider adsProvider) {
        super(adsProvider);
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    /*
     In order to avoid network resources race between IMA and Content CDN
     we prevent the prepare until AD is STARTED, No Pre-Roll or AD ERROR is received
    */
    @Override
    public void prepare(@NonNull final PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;

        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                log.d("IMA calling super.prepare");
                super.prepare(mediaConfig);
            } else {
                log.d("IMA setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getDuration();
        }

        return adsProvider.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
        }

        return adsProvider.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo is not enabled during AD playback");
            return;
        }

        adsProvider.seekTo(position);
    }

    @Override
    public void play() {
        log.d("PLAY IMA decorator");
        if (adsProvider != null && !adsProvider.isAdError()) {
            log.d("PLAY IMA decorator isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted = " + adsProvider.isAllAdsCompleted());
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
        if (adsProvider.isAdDisplayed()) {
            return;
        }
        log.d("IMA decorator Calling player play");
        getView().showVideoSurface();
        super.play();

    }

    @Override
    public void pause() {
        boolean isAdDisplayed = adsProvider.isAdDisplayed();
        log.d("PAUSE IMA decorator isAdDisplayed = " + isAdDisplayed + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted " + adsProvider.isAllAdsCompleted());
        if (isAdDisplayed && !adsProvider.isAdError()) {
            adsProvider.pause();
        }
        if (super.isPlaying()) {
            log.d("IMA decorator Calling content player pause");
            super.pause();
        }
    }

    @Override
    public void stop() {
        log.d("AdEnabled IMA stop");
        if (adsProvider != null) {
            adsProvider.destroyAdsManager();
        }
        super.stop();
    }

    @Override
    public boolean isPlaying() {
        log.d("AdEnabled isPlaying");
        if (adsProvider != null && adsProvider.isAdDisplayed()) {
            return !adsProvider.isAdPaused();
        }
        return super.isPlaying();
    }

    @Override
    public void skipAd() {
        if (adsProvider != null && !adsProvider.isAdError()) {
            adsProvider.skipAd();
        }
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
    public <T extends PKController> T getController(Class<T> type) {
        if (type == AdDAIEnabledPlayerController.class) {
            return (T) this;
        }
        return super.getController(type);
    }

    @Override
    public void onAdLoadingFinished() {
        log.d("onAdLoadingFinished pkPrepareReason");
        if (mediaConfig == null) {
            log.e("IMA onAdLoadingFinished mediaConfig == null");
            return;
        }
        prepare(mediaConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
    }
}
