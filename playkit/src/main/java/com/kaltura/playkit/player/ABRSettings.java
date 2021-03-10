package com.kaltura.playkit.player;

import androidx.annotation.NonNull;

public class ABRSettings {

    /**
     * Set minVideoBitrate in ABR
     *
     * @param minVideoBitrate - minimum video bitrate in ABR
     * @return - Player Settings.
     */
    private Long minVideoBitrate = Long.MIN_VALUE;
    /**
     * Set maxVideoBitrate in ABR
     *
     * @param maxVideoBitrate - maximum video bitrate in ABR
     * @return - Player Settings.
     */
    private Long maxVideoBitrate = Long.MAX_VALUE;
    /**
     * Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth
     * estimate is unavailable.
     *
     * @param initialBitrateEstimate The initial bitrate estimate in bits per second.
     * @return - Player Settings.
     */
    private Long initialBitrateEstimate;

    /**
     * Reset the ABR Settings.
     */
    public final static ABRSettings RESET = new ABRSettings().setMinVideoBitrate(Long.MIN_VALUE).setMaxVideoBitrate(Long.MAX_VALUE);

    public ABRSettings setMinVideoBitrate(@NonNull long minVideoBitrate) {
        this.minVideoBitrate = minVideoBitrate;
        return this;
    }

    public ABRSettings setMaxVideoBitrate(@NonNull long maxVideoBitrate) {
        this.maxVideoBitrate = maxVideoBitrate;
        return this;
    }

    /**
     * Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth
     * estimate is unavailable.
     *
     * <br>
     * <br>
     * If App is using {@link com.kaltura.playkit.Player#updateABRSettings(ABRSettings)}
     * <br>
     * Then Using {@link ABRSettings#setInitialBitrateEstimate(long)} is unaffected because
     * initial bitrate is only meant at the start of the playback
     * <br>
     *
     * @param initialBitrateEstimate The initial bitrate estimate in bits per second.
     * @return
     */
    public ABRSettings setInitialBitrateEstimate(@NonNull long initialBitrateEstimate) {
        this.initialBitrateEstimate = initialBitrateEstimate;
        return this;
    }

    public Long getMinVideoBitrate() {
        return minVideoBitrate;
    }

    public Long getMaxVideoBitrate() {
        return maxVideoBitrate;
    }

    public Long getInitialBitrateEstimate() {
        return initialBitrateEstimate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ABRSettings that = (ABRSettings) o;

        if (!minVideoBitrate.equals(that.minVideoBitrate)) return false;
        if (!maxVideoBitrate.equals(that.maxVideoBitrate)) return false;
        return initialBitrateEstimate.equals(that.initialBitrateEstimate);
    }

    @Override
    public int hashCode() {
        int result = minVideoBitrate.hashCode();
        result = 31 * result + maxVideoBitrate.hashCode();
        result = 31 * result + initialBitrateEstimate.hashCode();
        return result;
    }
}
