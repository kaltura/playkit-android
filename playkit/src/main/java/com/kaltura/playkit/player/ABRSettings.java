package com.kaltura.playkit.player;

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

    public ABRSettings setMinVideoBitrate(long minVideoBitrate) {
        this.minVideoBitrate = minVideoBitrate;
        return this;
    }

    public ABRSettings setMaxVideoBitrate(long maxVideoBitrate) {
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
    public ABRSettings setInitialBitrateEstimate(long initialBitrateEstimate) {
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

}
