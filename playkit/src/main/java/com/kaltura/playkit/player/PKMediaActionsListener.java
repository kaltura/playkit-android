package com.kaltura.playkit.player;

import com.kaltura.playkit.PKMediaConfig;

public interface PKMediaActionsListener {
    default void onUpdateMedia(PKMediaConfig mediaConfig) {}
    default void onStoppingMedia() {}

    PKMediaActionsListener Null = new PKMediaActionsListener() {};
}
