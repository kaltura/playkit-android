package com.kaltura.playkit;

import android.os.Bundle;

import com.kaltura.playkit.plugin.connect.OnCompletion;

public interface MediaEntryProvider {

    MediaEntry getMediaEntry();

    void load(String ks, String id, Bundle args, OnCompletion callback);
}
