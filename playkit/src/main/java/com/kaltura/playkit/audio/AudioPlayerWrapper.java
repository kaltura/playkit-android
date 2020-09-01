package com.kaltura.playkit.audio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.kaltura.android.exoplayer2.SimpleExoPlayer;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.BaseTrack;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.PlayerEngine;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.metadata.PKMetadata;

import java.util.List;

public class AudioPlayerWrapper implements AudioPlayerEngine {

    private MediaBrowserCompat mMediaBrowser;
    private MediaSessionCompat mSession;
    private Context context;
    private AudioServiceWrapper audioServiceWrapper;

    public AudioPlayerWrapper(AudioServiceWrapper audioServiceWrapper) {
        this.audioServiceWrapper = audioServiceWrapper;
    }

    public AudioPlayerWrapper(Context context, SimpleExoPlayer player) {
        this.context = context;
        mMediaBrowser = new MediaBrowserCompat(context,
                new ComponentName(context, AudioOnlyService.class), mConnectionCallback, null);
      //  mSession = new MediaSessionCompat(context, "AudioOnlyService");
    }

    public void initAudioWrapper() {
//        if (mMediaBrowser != null) {
//            mMediaBrowser.connect();
//        }
//        mSession.setCallback(new MediaSessionCallback());
//        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
//                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
//        mSession.setActive(true);
        mMediaBrowser.connect();
        context.startService(new Intent(context, AudioOnlyService.class));
    }

    @Override
    public void play() {
        Log.e("Gourav" ," in AudioPlayerWrapper Play ");

       // audioServiceWrapper.play();
    }

    @Override
    public void pause() {
      //  audioServiceWrapper.pause();
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.e("Gourav" , "onConnected");
                }
            };
}
