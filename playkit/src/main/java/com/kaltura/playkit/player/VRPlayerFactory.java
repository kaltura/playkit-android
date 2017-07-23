package com.kaltura.playkit.player;


import android.content.Context;

public interface VRPlayerFactory {

    VRPlayerEngine newInstance(Context context, PlayerEngine player);
}
