package com.kaltura.playkit;


import com.kaltura.playkit.backend.OnMediaLoadCompletion;

public interface MediaEntryProvider {

    void load(OnMediaLoadCompletion completion);

    void cancel();
}
