package com.kaltura.playkit.ads;

import com.kaltura.playkit.plugins.ads.PKPrepareReason;

/**
 * Created by gilad.nadav on 25/01/2017.
 */
public interface PKAdProviderListener {
    void onAdLoadingFinished(PKPrepareReason pkPrepareReason);
}
