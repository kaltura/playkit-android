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

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.List;


public class AdEnabledPlayerController extends PlayerDecorator implements AdController, PKAdProviderListener, PKController {

    private static final PKLog log = PKLog.get("AdEnablController");

    private AdsProvider adsProvider;
    private PKMediaConfig mediaConfig;

    public AdEnabledPlayerController(AdsProvider adsProvider) {
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

        return super.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
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
    public void seekToLiveDefaultPosition() {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekToLiveDefaultPosition is not enabled during AD playback");
            return;
        }
        super.seekToLiveDefaultPosition();
    }

    @Override
    public void setVolume(float volume) {
        if (adsProvider.isAdDisplayed()) {
            adsProvider.setVolume(volume);
            return;
        }

        super.setVolume(volume);
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
        return super.isPlaying();
    }

    @Override
    public void skip() {
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
    public boolean isAdDisplayed() {
        if (adsProvider != null) {
            return adsProvider.isAdDisplayed();
        }
        return false;
    }

    @Override
    public boolean isAdPlaying() {
        if (adsProvider != null) {
            return !adsProvider.isAdPaused();
        }
        return false;
    }

    @Override
    public boolean isAdError() {
        if (adsProvider != null) {
            return adsProvider.isAdError();
        }
        return false;
    }

    @Override
    public boolean isAllAdsCompleted() {
        if (adsProvider != null) {
            return adsProvider.isAllAdsCompleted();
        }
        return false;
    }

    @Override
    public PKAdInfo getAdInfo() {
        if (adsProvider != null) {
            return adsProvider.getAdInfo();
        }
        return null;
    }

    @Override
    public AdCuePoints getCuePoints() {
        if (adsProvider != null) {
            return adsProvider.getCuePoints();
        }
        return null;
    }

    @Override
    public PKAdPluginType getAdPluginType() {
        return PKAdPluginType.client;
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        if (type == AdEnabledPlayerController.class) {
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

    @Override
    public void advertisingPlayAdNow(String adTag) {
        if (adsProvider != null) {
            adsProvider.advertisingPlayAdNow(adTag);
        }
    }

    @Override
    public void setAdvertisingConfig(boolean isConfigured, @NonNull AdType adType, IMAEventsListener imaEventsListener) {
        if (adsProvider != null) {
            adsProvider.setAdvertisingConfig(isConfigured, adType, imaEventsListener);
        }
    }

    @Override
    public void advertisingSetCuePoints(List<Long> cuePoints) {
        if (adsProvider != null) {
            adsProvider.advertisingSetCuePoints(cuePoints);
        }
    }

    @Override
    public void advertisingSetAdInfo(PKAdvertisingAdInfo pkAdvertisingAdInfo) {
        if (adsProvider != null) {
            adsProvider.advertisingSetAdInfo(pkAdvertisingAdInfo);
        }
    }

    @Override
    public void advertisingPreparePlayer() {
        if (adsProvider != null) {
            adsProvider.advertisingPreparePlayer();
        }
    }
}
