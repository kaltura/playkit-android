package com.kaltura.playkit.audio;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.kaltura.playkit.Player;

import java.util.List;

public class AudioOnlyService extends MediaBrowserServiceCompat implements AudioServiceWrapper {

    private AudioPlayerWrapper audioPlayerWrapper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Gourav" ," in AudioOnlyService onCreate ");
        audioPlayerWrapper = new AudioPlayerWrapper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     //   setSessionToken(mSession.getSessionToken());
        return START_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("__ROOT__", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    public void setPlayer(Player player) {
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void play() {
        Log.e("Gourav" ," in AudioOnlyService Play ");
      //  startService(new Intent(getApplicationContext(), AudioOnlyService.class));
    }

    @Override
    public void pause() {

    }
}