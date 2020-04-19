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
        if (adsProvider != null) {
            if (!adsProvider.isContentPrepared() || adsProvider.isAdError() || (this.mediaSourceConfig != null && !this.mediaSourceConfig.equals(mediaSourceConfig))) {
                if ((this.mediaSourceConfig != null && !this.mediaSourceConfig.equals(mediaSourceConfig)) && !adsProvider.isAdRequested()) {
                    adsProvider.resetPluginFlags() ;
                }
                this.mediaSourceConfig = mediaSourceConfig;
            }

            if (adsProvider != null) {
                if (adsProvider.isAdRequested() || adsProvider.isAllAdsCompleted()) {
                    log.d("AdWrapper calling super.prepare");
                    super.load(this.mediaSourceConfig);
                } else {
                    log.d("AdWrapper setAdProviderListener");
                    adsProvider.setAdProviderListener(this);
                }
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
        if (adsProvider != null) {
            boolean isAdDisplayed = adsProvider.isAdDisplayed();
            log.d("AdWrapper PAUSE decorator isAdDisplayed = " + isAdDisplayed + " isAdPaused = " + adsProvider.isAdPaused() + " isAllAdsCompleted " + adsProvider.isAllAdsCompleted());
            if (isAdDisplayed && !adsProvider.isAdError()) {
                adsProvider.pause();
                return;
            }
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
        if (adsProvider != null) {
            AdCuePoints adCuePoints = adsProvider.getCuePoints();

            if (adCuePoints.getAdCuePoints() == null || adCuePoints.getAdCuePoints().isEmpty()) {
                return super.getCurrentPosition();
            }
            return adsProvider.getFakePlayerPosition(super.getCurrentPosition());
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getDuration() {
        if (adsProvider != null) {
            AdCuePoints adCuePoints = adsProvider.getCuePoints();

            if (adCuePoints.getAdCuePoints() == null || adCuePoints.getAdCuePoints().isEmpty()) {
                return super.getDuration();
            }
            return adsProvider.getFakePlayerDuration(super.getDuration());
        }
        return Consts.TIME_UNSET;
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider != null) {
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
    }

    @Override
    public boolean isPlaying() {
        log.d("AdWrapper isPlaying");
        return super.isPlaying();
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
