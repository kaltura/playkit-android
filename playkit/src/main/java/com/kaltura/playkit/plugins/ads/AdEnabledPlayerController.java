package com.kaltura.playkit.plugins.ads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kaltura.playkit.PKAdInfo;
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
    public final String  TAG = "AdEnablController";

    //IMASimplePlugin imaSimplePlugin;
    AdsProvider adsProvider;
    public AdEnabledPlayerController(AdsProvider adsProvider) {//(IMASimplePlugin imaSimplePlugin) {
        Log.d(TAG, "Init AdEnabledPlayerController");
        this.adsProvider = adsProvider;
    }

    @Override
    public long getDuration() {
        return super.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return super.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        super.seekTo(position);
    }

    @Override
    public void play() {
        Log.d(TAG, "AdEnabledPlayerController PLAY");
        if (!adsProvider.isAdDisplayed() && adsProvider.isAdRequested()) {
            super.play();
        } else if (adsProvider.isAdDisplayed()) {
            adsProvider.start(false);
        } else {
            super.pause();
            if (!adsProvider.isAdRequested()) {
                adsProvider.requestAd();
            }
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "AdEnabledPlayerController PAUSE");
        if (!adsProvider.isAdDisplayed()) {
            super.pause();
        } else {
            adsProvider.pause();
        }
    }

    @Override
    public void restore() {
        if (!adsProvider.isAdPaused()) {
            Log.d(TAG, "AdEnabledPlayerController RESTORE");
            super.restore();
        }
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
            adsProvider.requestAd();
        }
    }
}
