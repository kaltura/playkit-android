package com.kaltura.playkit.player;

import android.content.Context;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

public interface VRPlayerFactory {

    VRPlayerEngine newInstance(Context context, PlayerEngine player);
}
