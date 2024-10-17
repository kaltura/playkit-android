package com.kaltura.android.exoplayer2.upstream;

import com.kaltura.androidx.media3.exoplayer.upstream.BandwidthMeter;

public interface KBandwidthMeter extends BandwidthMeter {
    void resetBitrateEstimate();
}
