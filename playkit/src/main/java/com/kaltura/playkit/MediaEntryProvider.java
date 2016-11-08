package com.kaltura.playkit;

import com.kaltura.playkit.plugin.connect.OnCompletion;

public interface MediaEntryProvider {

    PKMediaEntry getMediaEntry();

    void load(OnCompletion callback);
}
