package com.kaltura.playkit;

import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);

    void cancel();
}
