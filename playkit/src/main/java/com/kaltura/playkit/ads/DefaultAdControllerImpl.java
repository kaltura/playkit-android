package com.kaltura.playkit.ads;

import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

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
                return;
            } else if (isAdDisplayed()) {
                adsProvider.resume();
                return;
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
        if (adsProvider != null) {
            return adsProvider.isAdDisplayed();
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
}


