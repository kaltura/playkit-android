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
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayerEngineWrapper;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;

/**
 * @hide
 */

public class AdsPlayerEngineWrapper extends PlayerEngineWrapper implements PKAdProviderListener {

    private static final PKLog log = PKLog.get("AdsPlayerEngineWrapper");

    private Context context;
    private AdsProvider adsProvider;
    private PKMediaSourceConfig mediaSourceConfig;
    private DefaultAdControllerImpl defaultAdController;
    private DefaultAdvertisingControllerImpl defaultAdvertisingController;

    public AdsPlayerEngineWrapper(final Context context, AdsProvider adsProvider) {
        this.context = context;
        this.adsProvider = adsProvider;
        this.defaultAdController = new DefaultAdControllerImpl(adsProvider);
        this.defaultAdvertisingController = new DefaultAdvertisingControllerImpl(adsProvider);
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        if (this.mediaSourceConfig != null && !this.mediaSourceConfig.equals(mediaSourceConfig)) {
            log.d("AdWrapper Load New Media");
            adsProvider.resetPluginFlags();
        }

        this.mediaSourceConfig = mediaSourceConfig;
        if (adsProvider != null) {
            //incase no ads provided - need to prepare so treating load state as ad was requested
            if (adsProvider.getCuePoints() != null && adsProvider.getCuePoints().getAdCuePoints() != null && adsProvider.getCuePoints().getAdCuePoints().size() == 0) {
                adsProvider.setAdRequested(true); // need to prepare immediately
            }

            if (preparePlayerForPlayback() || preparePlayerForPlaybackIfLiveMedia()) {
                log.d("AdWrapper calling super.prepare");
                super.load(mediaSourceConfig);
            } else {
                log.d("AdWrapper setAdProviderListener");
                adsProvider.setAdProviderListener(this);
            }
        }
    }

    private boolean preparePlayerForPlayback() {

        return (adsProvider.isAdRequested() && adsProvider.isForceSinglePlayerRequired()) ||
                (adsProvider.isAdRequested() && (adsProvider.getCuePoints() == null || adsProvider.getAdInfo() == null)) ||
                adsProvider.isAllAdsCompleted() || adsProvider.isAdError() || adsProvider.isAdDisplayed() ||
                adsProvider.isAdRequested() && adsProvider.getCuePoints() != null && (!adsProvider.getCuePoints().hasPreRoll() || getCurrentPosition() > 0) ||
                adsProvider.getPlaybackStartPosition() != null && adsProvider.getPlaybackStartPosition() > 0 && !adsProvider.isAlwaysStartWithPreroll();
    }

    /**
     * This check is only for Live Medias
     * Because for live media, player always seeks to live edge
     * when app comes from background which results the getCurrentPosition to 0
     *
     * @return boolean player preparation required or not
     */
    private boolean preparePlayerForPlaybackIfLiveMedia() {
        return isLiveMediaWithoutDvr() && getCurrentPosition() == 0 &&
                adsProvider.isAdvertisingConfigured() && adsProvider.isAdRequested() && !adsProvider.isAdDisplayed();
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
            if (adsProvider.isAdDisplayed()) {
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
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        if (resizeMode == null) {
            log.e("Resize mode is invalid");
            return;
        }

        if (adsProvider != null) {
            adsProvider.updateSurfaceAspectRatioResizeMode(resizeMode);
        }

        super.updateSurfaceAspectRatioResizeMode(resizeMode);
    }

    @Override
    public long getCurrentPosition() {
        return super.getCurrentPosition();
    }

    private boolean isLiveMediaWithoutDvr() {
        if (mediaSourceConfig != null) {
            return mediaSourceConfig.getMediaEntryType() == PKMediaEntry.MediaEntryType.Live;
        }
        return false;
    }

    @Override
    public long getProgramStartTime() {
        return super.getProgramStartTime();
    }

    @Override
    public long getDuration() {
        return super.getDuration();
    }

    @Override
    public void seekTo(long position) {
        log.d("AdWrapper seekTo");
        super.seekTo(position);
    }

    @Override
    public void seekToDefaultPosition() {
        log.d("AdWrapper seekToDefaultPosition");
        super.seekToDefaultPosition();
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
    public void setInputFormatChangedListener(Boolean enableListener) {
        super.setInputFormatChangedListener(enableListener);
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
        if (type == AdController.class && defaultAdController != null) {
            return (T) this.defaultAdController;
        }

        if (type == AdvertisingController.class && defaultAdvertisingController != null) {
            return (T) this.defaultAdvertisingController;
        }

        return super.getController(type);
    }

    @Override
    public void onAdLoadingFinished() {
        log.d("onAdLoadingFinished pkPrepareReason");
        if (mediaSourceConfig == null) {
            log.e("AdWrapper onAdLoadingFinished mediaSourceConfig == null");
            return;
        }
        load(mediaSourceConfig);
        if (adsProvider != null) {
            adsProvider.removeAdProviderListener();
        }
    }
}
