package com.kaltura.android.exoplayer2.upstream;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.kaltura.android.exoplayer2.util.Clock;


public class KDefaultBandwidthMeter implements KBandwidthMeter, TransferListener {

    private final DefaultBandwidthMeter wrappedDefaultBandwidthMeter;

    @Nullable
    private final Long initialBitrateEstimate;

    @Nullable
    private Long bitrateEstimate;

    private KDefaultBandwidthMeter(DefaultBandwidthMeter defaultBandwidthMeter, @Nullable Long initialBitrateEstimate) {
        this.wrappedDefaultBandwidthMeter = defaultBandwidthMeter;
        this.initialBitrateEstimate = initialBitrateEstimate;
        bitrateEstimate = null;
    }

    public void resetBitrateEstimate() {
        bitrateEstimate = initialBitrateEstimate;
    }

    @Override
    public long getBitrateEstimate() {
        if (bitrateEstimate != null) {
            return bitrateEstimate;
        }
        return wrappedDefaultBandwidthMeter.getBitrateEstimate();
    }

    @Nullable
    @Override
    public TransferListener getTransferListener() {
        return this;
    }

    @Override
    public void addEventListener(Handler handler, EventListener eventListener) {
        wrappedDefaultBandwidthMeter.addEventListener(handler, eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        wrappedDefaultBandwidthMeter.removeEventListener(eventListener);
    }

    @Override
    public void onTransferInitializing(DataSource dataSource, DataSpec dataSpec, boolean b) {
        wrappedDefaultBandwidthMeter.onTransferInitializing(dataSource, dataSpec, b);
    }

    @Override
    public void onTransferStart(DataSource dataSource, DataSpec dataSpec, boolean b) {
        wrappedDefaultBandwidthMeter.onTransferStart(dataSource, dataSpec, b);
    }

    @Override
    public void onBytesTransferred(DataSource dataSource, DataSpec dataSpec, boolean b, int i) {
        wrappedDefaultBandwidthMeter.onBytesTransferred(dataSource, dataSpec, b, i);
    }

    @Override
    public void onTransferEnd(DataSource dataSource, DataSpec dataSpec, boolean b) {
        wrappedDefaultBandwidthMeter.onTransferEnd(dataSource, dataSpec, b);
        bitrateEstimate = null;
    }

    public static final class Builder {
        private final DefaultBandwidthMeter.Builder wrappedBuilder;

        @Nullable
        private Long initialBitrateEstimate = null;

        public Builder(Context context) {
            wrappedBuilder = new DefaultBandwidthMeter.Builder(context);
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setSlidingWindowMaxWeight(int slidingWindowMaxWeight) {
            wrappedBuilder.setSlidingWindowMaxWeight(slidingWindowMaxWeight);
            return this;
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setInitialBitrateEstimate(long initialBitrateEstimate) {
            this.initialBitrateEstimate = initialBitrateEstimate;
            wrappedBuilder.setInitialBitrateEstimate(initialBitrateEstimate);
            return this;
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setInitialBitrateEstimate(int networkType, long initialBitrateEstimate) {
            wrappedBuilder.setInitialBitrateEstimate(networkType, initialBitrateEstimate);
            return this;
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setInitialBitrateEstimate(String countryCode) {
            wrappedBuilder.setInitialBitrateEstimate(countryCode);
            return this;
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setClock(Clock clock) {
            wrappedBuilder.setClock(clock);
            return this;
        }

        @CanIgnoreReturnValue
        public KDefaultBandwidthMeter.Builder setResetOnNetworkTypeChange(boolean resetOnNetworkTypeChange) {
            wrappedBuilder.setResetOnNetworkTypeChange(resetOnNetworkTypeChange);
            return this;
        }

        public KDefaultBandwidthMeter build() {
            return new KDefaultBandwidthMeter(wrappedBuilder.build(), initialBitrateEstimate);
        }
    }
}
