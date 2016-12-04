package com.kaltura.playkit.ads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;

import java.util.List;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator {

    private static final PKLog log = PKLog.get("AdEnablController");

    AdsProvider adsProvider;
    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            long adDuration = adsProvider.getDuration();
            log.v("getDuration: " + adDuration);
           return 1000 * adDuration;
        } else {
            return super.getDuration();
        }
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            long adPosition = adsProvider.getCurrentPosition();
            log.v("getCurrentPosition = " + adPosition);
            return 1000 * adPosition;
        } else {
            return super.getCurrentPosition();
        }
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo is not enabled during AD playback");
            return;
        } else {
            super.seekTo(position);
        }
    }

    @Override
    public void play() {
        log.d("PLAY isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
        if (adsProvider != null) {
            if (!adsProvider.isAdRequested()) {
                adsProvider.init();
                return;
            } else if (adsProvider.isAdDisplayed()) {
                adsProvider.resume();
                return;
            }
        }
        super.play();

    }

    @Override
    public void pause() {
        log.d("PAUSE isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
        if (adsProvider.isAdDisplayed()) {
            adsProvider.pause();
        } else {
            super.pause();
        }
    }

    @Override
    public void onApplicationResumed() {
        super.onApplicationResumed();
        if (adsProvider.isAdDisplayed()) {
            log.d("onApplicationResumed");
            adsProvider.resume();
        }

    }

    @Override
    public void onApplicationPaused() {
        log.d("onApplicationPaused");
        if (adsProvider.isAdDisplayed()) {
            adsProvider.pause();
        }
        super.onApplicationPaused();
    }

    @Override
    public PKAdInfo getAdInfo() {
        return adsProvider.getAdInfo();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        if (value == null) {
            return;
        }
        if (adsProvider != null) {
            if (key.equals(IMAConfig.AD_TAG_LANGUAGE)) {
                adsProvider.getAdsConfig().setLanguage((String) value);
            } else if (key.equals(IMAConfig.AD_TAG_URL)) {
                adsProvider.getAdsConfig().setAdTagURL((String) value);
            } else if (key.equals(IMAConfig.ENABLE_BG_PLAYBACK)) {
                adsProvider.getAdsConfig().setEnableBackgroundPlayback((boolean) value);
            } else if (key.equals(IMAConfig.AUTO_PLAY_AD_BREAK)) {
                adsProvider.getAdsConfig().setAutoPlayAdBreaks((boolean) value);
            } else if (key.equals(IMAConfig.AD_VIDEO_BITRATE)) {
                adsProvider.getAdsConfig().setVideoBitrate((int) value);
            } else if (key.equals(IMAConfig.VIDEO_MIME_TYPES)) {
                adsProvider.getAdsConfig().setVideoMimeTypes((List<String>) value);
            }
        }
    }
}
