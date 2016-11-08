package com.kaltura.playkit;

import com.kaltura.playkit.plugins.mediaproviders.base.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);
}
