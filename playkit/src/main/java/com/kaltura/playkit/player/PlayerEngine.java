package com.kaltura.playkit.player;

import android.net.Uri;
import android.view.View;

import com.kaltura.playkit.TracksInfo;


/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public interface PlayerEngine {

    void load(Uri mediaSourceUri, String licenseUri);

    View getView();

    void play();

    void pause();

    long getCurrentPosition();

    void seekTo(long position);

    void startFrom(long position);

    void setEventListener(PlayerController.EventListener eventTrigger);

    void setStateChangedListener(PlayerController.StateChangedListener stateChangedTrigger);

    long getDuration();

    long getBufferedPosition();

    void release();

    void restore();

    void destroy();

    void changeTrack(String uniqueId);

    TracksInfo getTracksInfo();

    long getCurrentVideoBitrate();
}
