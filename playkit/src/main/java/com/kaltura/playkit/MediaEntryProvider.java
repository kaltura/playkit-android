package com.kaltura.playkit;

import com.kaltura.playkit.plugins.mediaprovider.base.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);
}
