package com.kaltura.playkit.player;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

class SourceSelector {
    static PKMediaSource selectSource(PKMediaEntry mediaEntry) {
        // TODO: really implement this.
        return mediaEntry.getSources().get(0);
    }
}

