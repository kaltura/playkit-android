package com.kaltura.playkit.player;

import android.net.Uri;

import com.kaltura.playkit.PKMediaSource;

/**
 * Created by anton.afanasiev on 18/07/2017.
 */

public interface SourceConfig {

    PKMediaSource getSource();

    boolean isCea608CaptionsEnabled();

    boolean useTextureView();

    Uri getUrl();
}
