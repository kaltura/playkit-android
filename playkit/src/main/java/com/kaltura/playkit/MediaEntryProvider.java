package com.kaltura.playkit;

import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);

    void cancel();
}
