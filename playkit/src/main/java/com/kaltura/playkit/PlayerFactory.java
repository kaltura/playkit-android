package com.kaltura.playkit;

import android.content.Context;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */
public class PlayerFactory {
    public static Player newPlayer(Context context) {
        return new POCPlayer(context);
    }
}
