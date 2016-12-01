package com.kaltura.playkit.plugins.ads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerDecorator;

import java.util.List;

import static com.kaltura.playkit.plugins.ads.AdsConfig.AD_TAG_LANGUAGE;
import static com.kaltura.playkit.plugins.ads.AdsConfig.AD_TAG_URL;
import static com.kaltura.playkit.plugins.ads.AdsConfig.AD_VIDEO_BITRATE;
import static com.kaltura.playkit.plugins.ads.AdsConfig.AUTO_PLAY_AD_BREAK;
import static com.kaltura.playkit.plugins.ads.AdsConfig.ENABLE_BG_PLAYBACK;
import static com.kaltura.playkit.plugins.ads.AdsConfig.VIDEO_MIME_TYPES;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator {
    public static final String  TAG = "AdEnablController";
    private static final PKLog log = PKLog.get(TAG);

    //IMASimplePlugin imaSimplePlugin;
    AdsProvider adsProvider;
    public AdEnabledPlayerController(AdsProvider adsProvider) {
        log.d("Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    @Override
    public long getDuration() {
        if (adsProvider.isAdDisplayed()) {
            log.d("getDuration: " + adsProvider.getDuration());
           return 1000 * adsProvider.getDuration();
        } else {
            return super.getDuration();
        }
    }

    @Override
    public long getCurrentPosition() {
        if (adsProvider.isAdDisplayed()) {
            log.d("getCurrentPosition = " + adsProvider.getCurrentPosition());
            return 1000 * adsProvider.getCurrentPosition();
        } else {
            return super.getCurrentPosition();
        }
    }

    @Override
    public void seekTo(long position) {
        if (adsProvider.isAdDisplayed()) {
            log.d("seekTo: ss not enabled during AD playback");
            return;
        } else {
            super.seekTo(position);
        }
    }

    @Override
    public void play() {
        log.d("Ad Event AdEnabledPlayerController PLAY isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
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
        log.d("Ad Event AdEnabledPlayerController PAUSE isAdDisplayed = " + adsProvider.isAdDisplayed() + " isAdPaused = " + adsProvider.isAdPaused());
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
            log.d("AdEnabledPlayerController onApplicationResumed");
            adsProvider.resume();
        }

    }

    @Override
    public void onApplicationPaused() {
        log.d("AdEnabledPlayerController onApplicationPaused");
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
            if (adsProvider.getPluginName().equals(pluginName)) {
                if (key.equals(AD_TAG_LANGUAGE)) {
                    adsProvider.getAdsConfig().setLanguage((String) value);
                } else if (key.equals(AD_TAG_URL)) {
                    adsProvider.getAdsConfig().setAdTagUrl((String) value);
                } else if (key.equals(ENABLE_BG_PLAYBACK)) {
                    adsProvider.getAdsConfig().setEnableBackgroundPlayback((boolean) value);
                } else if (key.equals(AUTO_PLAY_AD_BREAK)) {
                    adsProvider.getAdsConfig().setAutoPlayAdBreaks((boolean) value);
                } else if (key.equals(AD_VIDEO_BITRATE)) {
                    adsProvider.getAdsConfig().setVideoBitrate((int) value);
                } else if (key.equals(VIDEO_MIME_TYPES)) {
                    adsProvider.getAdsConfig().setVideoMimeTypes((List<String>) value);
                }
            }
        }
    }
}
