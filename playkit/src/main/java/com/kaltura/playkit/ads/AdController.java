package com.kaltura.playkit.ads;

/**
 * Created by Noam Tamim @ Kaltura on 14/12/2016.
 */
public interface AdController {
    void skipAd();
    long getAdCurrentPosition();
    long getAdDuration();
}
