package com.kaltura.playkit;

import com.kaltura.playkit.plugin.mediaprovider.base.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);
}
