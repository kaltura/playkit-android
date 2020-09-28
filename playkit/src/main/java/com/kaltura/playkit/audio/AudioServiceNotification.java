package com.kaltura.playkit.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.R;

public class AudioServiceNotification extends BroadcastReceiver {

    private static final PKLog log = PKLog.get("AudioServiceNotification");

    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_STOP = "ACTION_STOP";

    public static final String CHANNEL_ID = "PLAYKIT_AUDIO_CHANNEL";

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private AudioService audioService;
    private NotificationManager mNotificationManager;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;

    private final PendingIntent mPlayIntent;
    private final PendingIntent mPauseIntent;
    private final PendingIntent mStopIntent;

    private boolean notificationStarted = false;

    public AudioServiceNotification(AudioService audioService) throws RemoteException {
        this.audioService = audioService;
        updateSessionToken();
        mNotificationManager = (NotificationManager) audioService.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = audioService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(audioService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(audioService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mStopIntent = PendingIntent.getBroadcast(audioService, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        log.d("Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
        }
    }

    public void startNotification() {
        if (!notificationStarted) {
            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                mController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                audioService.registerReceiver(this, filter);

                audioService.startForeground(NOTIFICATION_ID, notification);
                notificationStarted = true;
            }
        }
    }

    public void stopNotification() {
        if (notificationStarted) {
            notificationStarted = false;
            mController.unregisterCallback(mCb);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                audioService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            audioService.stopForeground(true);
        }
    }

    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = audioService.getSessionToken();
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            if (mSessionToken != null) {
                mController = new MediaControllerCompat(audioService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (notificationStarted) {
                    mController.registerCallback(mCb);
                }
            }
        }
    }

    private Notification createNotification() {
        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(audioService, CHANNEL_ID);

        addPlayPauseActions(notificationBuilder);

        notificationBuilder
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(mStopIntent)
                        .setMediaSession(mSessionToken))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("My Audio Service")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);

        return notificationBuilder.build();
    }

    private void addPlayPauseActions(final NotificationCompat.Builder notificationBuilder) {
        final String label;
        final int icon;
        final PendingIntent intent;
//        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
//            label = "Pause";
//            icon = android.R.drawable.ic_media_pause;
//            intent = mPauseIntent;
//        } else {
            label = "Play";
            icon = android.R.drawable.ic_media_play;
            intent = mPlayIntent;
     //   }
        notificationBuilder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            log.d("Received new playback state" + state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            log.d("Received new metadata = " + metadata);
            Notification notification = createNotification();
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            log.d( "Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                log.e("could not connect media controller Exception = " + e);
            }
        }
    };

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            "TODO Notification Channel",
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("TODO Notification Channel Description");

            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
