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

import com.kaltura.playkit.PlayerEngineWrapper;
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

/**
 * @hide
 */

public class AdsPlayerEngineWrapper extends PlayerEngineWrapper implements PKAdProviderListener {

    private static final PKLog log = PKLog.get("AdsPlayerEngineWrapper");

    private Context context;
    private AdsProvider adsProvider;
    private PKMediaSourceConfig mediaSourceConfig;

    public AdsPlayerEngineWrapper(final Context context, AdsProvider adsProvider) {
        this.context = context;
        this.adsProvider = adsProvider;
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        this.mediaSourceConfig = mediaSourceConfig;
        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                log.d("IMA calling super.prepare");
                super.load(mediaSourceConfig);
            } else {
                log.d("IMA setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    @Override
    public void play() {
        log.d("PLAY IMA decorator");
        if (adsProvider != null) {
            if (!adsProvider.isAdError()) {
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
            ///super.pause();
            log.d("XXXXXX1");
            return;
        }

        if (super.isPlaying()) {
            log.d("XXXXXX2");
            log.d("IMA decorator Calling content player pause");
            super.pause();
        }
    }

    @Override
    public long getCurrentPosition() {
        long currpos;
        if (adsProvider.isAdDisplayed()) {
            currpos = Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
            log.d("IMA -> XXXX getCurrentPosition = " + currpos);
        } else {
            currpos = super.getCurrentPosition();
            log.d("PLAYER -> XXXX getCurrentPosition = " + currpos);
        }
        return currpos;
    }

    @Override
    public long getDuration() {
        long playbackDuration;
        if (adsProvider.isAdDisplayed()) {
            playbackDuration = Consts.MILLISECONDS_MULTIPLIER * adsProvider.getDuration();
            log.d("IMA -> XXXX getDuration = " + playbackDuration);
        } else {
            playbackDuration = super.getDuration();
            log.d("PLAYER -> XXXX getDuration = " + playbackDuration);
        }
        return playbackDuration;
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
    public boolean isPlaying() {
        log.d("AdEnabled isPlaying");
        if (adsProvider != null && adsProvider.isAdDisplayed()) {
            return !adsProvider.isAdPaused();
        }
        return super.isPlaying();
    }

    //TDOD???
//    @Override
//    public void release() {
//
//    }

    @Override
    public void stop() {
        log.d("AdEnabled IMA stop");
        if (adsProvider != null) {
            adsProvider.destroyAdsManager();
        }
        super.stop();
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        // TODO: 12/12/18  which class to use
//        if (type == AdEnabledPlayerController.class) {
//            return (T) this;
//        }
        return super.getController(type);
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
