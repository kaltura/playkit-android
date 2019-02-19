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
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

/**
 * @hide
 */

public class AdsDAIPlayerEngineWrapper extends PlayerEngineWrapper implements PKAdProviderListener {

    private static final PKLog log = PKLog.get("DAIPlayerEngineWrapper");

    private Context context;
    private AdsProvider adsProvider;
    private PKMediaSourceConfig mediaSourceConfig;
    private DefaultDAIAdControllerImpl defaultDAIAdController;

    public AdsDAIPlayerEngineWrapper(final Context context, AdsProvider adsProvider) {
        this.context = context;
        this.adsProvider = adsProvider;
        this.defaultDAIAdController = new DefaultDAIAdControllerImpl(adsProvider);
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        this.mediaSourceConfig = mediaSourceConfig;
        if (adsProvider != null) {
            if (adsProvider.isAdRequested()) {
                log.d("AdWrapper calling super.prepare");
                super.load(mediaSourceConfig);
            } else {
                log.d("AdWrapper setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    @Override
    public void play() {
        log.d("AdWrapper PLAY");
        if (adsProvider != null) {
            if (!adsProvider.isAdError()) {
                log.d("AdWrapper PLAY isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted = " + adsProvider.isAllAdsCompleted());
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
            if (adsProvider.isAdDisplayed() && !adsProvider.isAdPaused()) {
                return;
            }
        }

        log.d("AdWrapper decorator Calling player play");
        getView().showVideoSurface();
        super.play();
    }

    @Override
    public void pause() {
        boolean isAdDisplayed = adsProvider.isAdDisplayed();
        log.d("AdWrapper PAUSE decorator isAdDisplayed = " + isAdDisplayed + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted " + adsProvider.isAllAdsCompleted());
        if (isAdDisplayed && !adsProvider.isAdError()) {
            adsProvider.pause();
            return;
        }

        if (super.isPlaying()) {
            log.d("AdWrapper decorator Calling content player pause");
            super.pause();
        }
    }

    @Override
    public void replay() {
        super.replay();
        seekTo(0);
    }

    @Override
    public long getCurrentPosition() {
        return adsProvider.getFakePlayerPosition(super.getCurrentPosition());
    }

    @Override
    public long getPositionInWindowMs() {
        return super.getPositionInWindowMs();
    }

    private long getFakePlayerPosition() {
        long playerPosition = super.getCurrentPosition();
        //log.d("playerPosition = " + playerPosition);
        if (playerPosition == Consts.POSITION_UNSET) {
            return 0;
        }
        AdCuePoints adCuePoints = adsProvider.getCuePoints();
        if (adCuePoints.getAdCuePoints().size() != adCuePoints.getDaiAdsList().size()) {
            return playerPosition;
        }

        long fakePos = playerPosition;
        for (int indx = 0 ; indx < adCuePoints.getDaiAdsList().size() ; indx++) {
            long cuePointPosition = adCuePoints.getDaiAdsList().get(indx).first;
            if (cuePointPosition <= playerPosition) {
                fakePos -= adCuePoints.getDaiAdsList().get(indx).second;
            }
        }
        if (fakePos < 0) {
            return 0;
        }
        //log.d("fakePos = " + fakePos);
        return fakePos;
    }

    @Override
    public long getProgramStartTime() {
        return super.getProgramStartTime();
    }

    @Override
    public long getDuration() {
        return getFakePlayerDuration();
    }

    private long getFakePlayerDuration() {
        long duration = super.getDuration();
        if (duration == Consts.TIME_UNSET) {
            return 0;
        }
        AdCuePoints adCuePoints = adsProvider.getCuePoints();

        if (adCuePoints.getAdCuePoints() == null || adCuePoints.getDaiAdsList() == null || adCuePoints.getAdCuePoints().size() != adCuePoints.getDaiAdsList().size()) {
            return duration;
        }

        long fakeDuration = duration;
        for (int indx = 0 ; indx < adCuePoints.getDaiAdsList().size() ; indx++) {
            fakeDuration -= adCuePoints.getDaiAdsList().get(indx).second;
        }
        if (fakeDuration < 0) {
            return 0;
        }
        //log.d("fakeDuration = " + fakeDuration);
        return fakeDuration;
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo is not enabled during AD playback");
            return;
        }
        boolean isPlaying = isPlaying();
        adsProvider.seekTo(position);
        if (!isPlaying) {
            pause();
        }
    }

    @Override
    public boolean isPlaying() {
        log.d("AdWrapper isPlaying");
        return super.isPlaying();
    }

    @Override
    public void setAnalyticsListener(AnalyticsListener analyticsListener) {
        super.setAnalyticsListener(analyticsListener);
    }

    @Override
    public void stop() {
        log.d("AdWrapper stop");
        if (adsProvider != null) {
            adsProvider.setAdRequested(false);
            adsProvider.destroyAdsManager();
        }
        super.stop();
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        if (type == AdController.class && defaultDAIAdController != null) {
            return (T) this.defaultDAIAdController;
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
            log.d("AdWrapper onAdLoadingFinished mediaSourceConfig == null");
            return;
        }
        load(mediaSourceConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
    }
}
