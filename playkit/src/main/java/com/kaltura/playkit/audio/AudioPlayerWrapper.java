package com.kaltura.playkit.audio;

import android.app.Notification;
import android.util.Log;

import com.kaltura.playkit.PKLog;

public class AudioPlayerWrapper {

    private static final PKLog log = PKLog.get("AudioPlayerWrapper");

    private AudioServiceInterface audioServiceInterface;
    private static AudioPlayerWrapper audioPlayerWrapper;
    
    AudioPlayerWrapper(AudioServiceInterface audioServiceInterface) {
        this.audioServiceInterface = audioServiceInterface;
    }

    void setAudioPlayerWrapper(AudioPlayerWrapper audioPlayerWrapper) {
        AudioPlayerWrapper.audioPlayerWrapper = audioPlayerWrapper;
    }

    public static AudioPlayerWrapper getInstance() {
        return audioPlayerWrapper;
    }

    public void play(Notification playerNotification) {
        log.d(" in AudioPlayerWrapper Play");
        audioServiceInterface.startForegroundAudioService(playerNotification);
    }

    public void pause() {
        log.d(" in AudioPlayerWrapper Pause");
        audioServiceInterface.stopAudioService();
    }
}
