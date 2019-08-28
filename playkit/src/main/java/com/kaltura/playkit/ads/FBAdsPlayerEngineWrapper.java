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

import android.content.Context;

import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerEngineWrapper;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;

/**
 * @hide
 */

public class FBAdsPlayerEngineWrapper extends PlayerEngineWrapper implements PKAdProviderListener {

    private static final PKLog log = PKLog.get("FBAdsPlayerEngineWrapper");

    private Context context;
    private AdsProvider adsProvider;
    private PKMediaSourceConfig mediaSourceConfig;
    private DefaultAdControllerImpl defaultAdController;

    public FBAdsPlayerEngineWrapper(final Context context, AdsProvider adsProvider) {
        this.context = context;
        this.adsProvider = adsProvider;
        this.defaultAdController = new DefaultAdControllerImpl(adsProvider);
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        this.mediaSourceConfig = mediaSourceConfig;
        if (adsProvider != null) {
            if (adsProvider.isAdDisplayed() || adsProvider.isAdRequested() && adsProvider.getCuePoints() != null && !adsProvider.getCuePoints().hasPreRoll()) {
                log.d("FB calling super.prepare");
                super.load(mediaSourceConfig);
            } else {
                log.d("FB setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    @Override
    public void play() {
        log.d("PLAY FB decorator");
        if (adsProvider != null) {
            if (!adsProvider.isAdError()) {
                log.d("PLAY FB decorator isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted = " + adsProvider.isAllAdsCompleted());
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
        }

        log.d("FB decorator Calling player play");
        getView().showVideoSurface();
        super.play();
    }

    @Override
    public void pause() {
        boolean isAdDisplayed = adsProvider.isAdDisplayed();
        log.d("PAUSE FB decorator isAdDisplayed = " + isAdDisplayed + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted " + adsProvider.isAllAdsCompleted());
        if (isAdDisplayed && !adsProvider.isAdError()) {
            adsProvider.pause();
            return;
        }

        if (super.isPlaying()) {
            log.d("IMA decorator Calling content player pause");
            super.pause();
        }
    }

    @Override
    public long getCurrentPosition() {
//        long currpos;
        log.d("FB  -> getCurrentPosition isAdDisplayed = " + adsProvider.isAdDisplayed());
        if (adsProvider.isAdDisplayed()) {
//            currpos = Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
            log.d("FB  -> getCurrentPosition = " + adsProvider.getCurrentPosition());
            adsProvider.getCurrentPosition();
        }
//        else {
//            currpos = super.getCurrentPosition();
//            //log.d("PLAYER -> getCurrentPosition = " + currpos);
//        }
//        return currpos;
        return super.getCurrentPosition();
    }

    @Override
    public long getProgramStartTime() {
        return super.getProgramStartTime();
    }

    @Override
    public long getDuration() {
//        long playbackDuration;
//        if (adsProvider.isAdDisplayed()) {
//            playbackDuration = Consts.MILLISECONDS_MULTIPLIER * adsProvider.getDuration();
//            //log.d("IMA -> getDuration = " + playbackDuration);
//        } else {
//            playbackDuration = super.getDuration();
//            //log.d("PLAYER -> getDuration = " + playbackDuration);
//        }
//        return playbackDuration;
        return super.getDuration();
    }

    @Override
    public void seekTo(long position) {
//        if (adsProvider.isAdDisplayed()) {
//            log.d("seekTo is not enabled during AD playback");
//            return;
//        }
        super.seekTo(position);
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
    public void setAnalyticsListener(AnalyticsListener analyticsListener) {
        super.setAnalyticsListener(analyticsListener);
    }

    @Override
    public void stop() {
        log.d("AdEnabled FB stop");
        if (adsProvider != null) {
            adsProvider.setAdRequested(false);
            adsProvider.destroyAdsManager();
        }
        super.stop();
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        if (type == AdController.class && defaultAdController != null) {
            return (T) this.defaultAdController;
        }
        return null;
    }

    @Override
    public void destroy() {
        if (adsProvider != null) {
            adsProvider.setAdRequested(false);
            adsProvider.destroyAdsManager();
            adsProvider = null;
        }
        super.destroy();
    }

    @Override
    public void onAdLoadingFinished() {
        log.d("onAdLoadingFinished pkPrepareReason");
        if (mediaSourceConfig == null) {
            log.e("IMA onAdLoadingFinished mediaSourceConfig == null");
            return;
        }
        load(mediaSourceConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
    }
}
