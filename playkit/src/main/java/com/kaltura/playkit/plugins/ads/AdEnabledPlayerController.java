package com.kaltura.playkit.plugins.ads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.plugins.ads.ima.IMASimplePlugin;

/**
 * Created by gilad.nadav on 20/11/2016.
 */

public class AdEnabledPlayerController extends PlayerDecorator {
    public final String  TAG = "AdEnablController";

    IMASimplePlugin imaSimplePlugin;
    public AdEnabledPlayerController(IMASimplePlugin imaSimplePlugin) {
        Log.d(TAG, "Init AdEnabledPlayerController");
        this.imaSimplePlugin = imaSimplePlugin;
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
        if (!imaSimplePlugin.ismIsAdDisplayed()) {
            //super.play();
        }

    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void restore() {
        super.restore();
    }

    @Override
    public PKAdInfo getAdInfo() {
        return super.getAdInfo();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        super.updatePluginConfig(pluginName, key, value);
    }
}
