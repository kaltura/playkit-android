package com.kaltura.playkit.audio;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.kaltura.playkit.PKLog;

import java.util.List;

public class AudioService extends MediaBrowserServiceCompat implements AudioServiceInterface {

    private static final PKLog log = PKLog.get("AudioService");

    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__";
    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;
    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.example.android.uamp.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    private boolean mAudioNoisyReceiverRegistered;

    private MediaSessionCompat mSession;
    private AudioServiceNotification audioServiceNotification;
    private AudioManager mAudioManager;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        log.d("Headphones disconnected.");
//                        if (isPlaying()) {
//                            Intent i = new Intent(context, AudioService.class);
//                            i.setAction(AudioService.ACTION_CMD);
//                            i.putExtra(AudioService.CMD_NAME, AudioService.CMD_PAUSE);
//                            getApplicationContext().startService(i);
//                        }
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mSession = new MediaSessionCompat(this, AudioService.class.getName());
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        AudioPlayerWrapper audioPlayerWrapper = new AudioPlayerWrapper(this);
        audioPlayerWrapper.setAudioPlayerWrapper(audioPlayerWrapper);

        // Notification Creation
        try {
            audioServiceNotification = new AudioServiceNotification(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
        // To grab audio focus
        this.mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        log.d(" in AudioService onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Do Nothing. We are passing the empty root id which will allow the clients to connect but will not allow
        // then to browse the content.
        return new BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Do Nothing if we allow clients to browse the content in `onGetRoot` then we need to put the logic here/
    }

    @Override
    public void startForegroundAudioService(Notification playerNotification) {
        tryToGetAudioFocus();
        startService(new Intent(getApplicationContext(), AudioService.class));
        //startForeground(15, playerNotification);
        audioServiceNotification.startNotification();
    }

    @Override
    public void stopAudioService() {
        audioServiceNotification.stopNotification();
        stopSelf();
        giveUpAudioFocus();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        audioServiceNotification.stopNotification();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mSession.release();
        audioServiceNotification.stopNotification();
    }

    private void tryToGetAudioFocus() {
        log.d("tryToGetAudioFocus");
        int result =
                mAudioManager.requestAudioFocus(
                        mOnAudioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED;
        } else {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void giveUpAudioFocus() {
        log.d("giveUpAudioFocus");
        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            focusChange -> {
                log.d("onAudioFocusChange. focusChange = " + focusChange);
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        mCurrentAudioFocusState = AUDIO_FOCUSED;
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                        mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // Lost audio focus, but will gain it back (shortly), so note whether
                        // playback should resume
                        mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Lost audio focus, probably "permanently"
                        mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                        break;
                }
            };

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            getApplicationContext().registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            getApplicationContext().unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            log.d("MediaSessionCallback play");
        }

        @Override
        public void onSkipToQueueItem(long queueId) {

        }

        @Override
        public void onSeekTo(long position) {

        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

        }

        @Override
        public void onPause() {
            log.d("MediaSessionCallback onPause");
        }

        @Override
        public void onStop() {
            log.d("MediaSessionCallback onStop");
        }

        @Override
        public void onSkipToNext() {

        }

        @Override
        public void onSkipToPrevious() {

        }
    }
}