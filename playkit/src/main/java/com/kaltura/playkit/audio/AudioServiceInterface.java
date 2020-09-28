package com.kaltura.playkit.audio;

import android.app.Notification;

public interface AudioServiceInterface {

    /**
     * Starts the AudioService which extends to MediaBrowserService
     * @param playerNotification Notification for the status bar
     */
    void startForegroundAudioService(Notification playerNotification);

    /**
     * Stops the AudioService
     */
    void stopAudioService();
}
