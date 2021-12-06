package com.kaltura.playkit.ads;

import androidx.annotation.NonNull;

import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.List;

public class DefaultAdControllerImpl implements AdController {

    private AdsProvider adsProvider;

    public DefaultAdControllerImpl(AdsProvider adsProvider) {
        this.adsProvider = adsProvider;
    }

    @Override
    public void skip() {
        if (adsProvider != null) {
            adsProvider.skipAd();
        }
    }

    @Override
    public void play() {
        if (adsProvider == null) {
            return;
        }
        if (!adsProvider.isAllAdsCompleted()) {
            if (!adsProvider.isAdRequested()) {
                adsProvider.start();
            } else if (isAdDisplayed()) {
                adsProvider.resume();
            }
        }
    }

    @Override
    public void pause() {
        if (isAdDisplayed() && !isAdError()) {
            adsProvider.pause();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void seekTo(long position) {
       //seeking operation during ad is blocked
    }

    @Override
    public void setVolume(float volume) {
        //control playback volume [0..1.0]
        if (adsProvider.isAdDisplayed()) {
            adsProvider.setVolume(volume);
        }
    }

    @Override
    public long getAdCurrentPosition() {
        if (adsProvider != null && isAdDisplayed()) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getCurrentPosition();
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getAdDuration() {
        if (adsProvider != null && adsProvider.getDuration() != Consts.TIME_UNSET && isAdDisplayed()) {
            return Consts.MILLISECONDS_MULTIPLIER * adsProvider.getDuration();
        }
        return Consts.TIME_UNSET;
    }

    @Override
    public boolean isAdDisplayed() {
        return adsProvider != null && adsProvider.isAdDisplayed();
    }

    @Override
    public boolean isAdPlaying() {
        return adsProvider != null && adsProvider.isAdDisplayed() && !adsProvider.isAdPaused();
    }

    @Override
    public boolean isAdError() {
        return adsProvider != null && adsProvider.isAdError();
    }

    @Override
    public boolean isAllAdsCompleted() {
        return adsProvider != null && adsProvider.isAllAdsCompleted();
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
        if (adsProvider != null) {
            return adsProvider.getAdPluginType();
        }
        return null;
    }

    @Override
    public void playAdNow(String adTag) {
        if (adsProvider != null) {
            adsProvider.playAdNow(adTag);
        }
    }

    @Override
    public void setAdvertisingConfig(boolean isConfigured, @NonNull AdType adType, IMAEventsListener imaEventsListener) {
        if (adsProvider != null) {
            adsProvider.setAdvertisingConfig(isConfigured, adType, imaEventsListener);
        }
    }

    @Override
    public void setCuePoints(List<Long> cuePoints) {
        if (adsProvider != null) {
            adsProvider.setCuePoints(cuePoints);
        }
    }

    @Override
    public void adControllerPreparePlayer() {
        if (adsProvider != null) {
            adsProvider.adControllerPreparePlayer();
        }
    }

    @Override
    public void setAdInfo(PKAdvertisingAdInfo pkAdvertisingAdInfo) {
        if (adsProvider != null) {
            adsProvider.setAdInfo(pkAdvertisingAdInfo);
        }
    }
}


