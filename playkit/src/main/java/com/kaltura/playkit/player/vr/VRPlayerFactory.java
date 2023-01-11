package com.kaltura.playkit.player.vr;

import android.content.Context;

import androidx.annotation.Nullable;

import com.kaltura.playkit.player.BaseExoplayerView;
import com.kaltura.playkit.player.PlayerEngine;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

public interface VRPlayerFactory {

    PlayerEngine newInstance(Context context, PlayerEngine player, @Nullable VRSettings vrSettings);

    BaseExoplayerView newVRViewInstance(Context context);
}
