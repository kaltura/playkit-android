package com.kaltura.playkit.player.simid;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import tv.broadpeak.simid.controller.SimidController;

public class KalturaSimidController extends SimidController {

    private SimidControllerProxy controller;

    public void setController(SimidControllerProxy controller) {
        this.controller = controller;
    }

    public KalturaSimidController(@NonNull Activity activity, @NonNull Context context, @NonNull Rect mainPlayerDimensions, @NonNull String creativeUri, @NonNull String adParameters, float adDuration, boolean adSkippable) {
        super(activity, context, mainPlayerDimensions, creativeUri, adParameters, adDuration, adSkippable);
    }

    @Override
    protected void receiveMessage(@NonNull String messageStr) {
        super.receiveMessage(messageStr);
        controller.receiveMessage(messageStr);
    }

    @Override
    protected void postMessage(@NonNull String message) {
        super.postMessage(message);
        controller.postMessage(message);
    }
}
