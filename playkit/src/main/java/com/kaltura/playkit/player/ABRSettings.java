package com.kaltura.playkit.player;

public class ABRSettings {

    private Long initialBitrateEstimate = null;
    private Long maxVideoBitrate = Long.MAX_VALUE;
    private Long minVideoBitrate = Long.MIN_VALUE;
    private Long maxVideoHeight = Long.MAX_VALUE;
    private Long minVideoHeight = Long.MIN_VALUE;
    private Long maxVideoWidth = Long.MAX_VALUE;
    private Long minVideoWidth = Long.MIN_VALUE;

    /**
     * Reset the ABR Settings.
     */
    public final static ABRSettings RESET = new ABRSettings()
            .setMinVideoBitrate(Long.MIN_VALUE)
            .setMaxVideoBitrate(Long.MAX_VALUE)
            .setMinVideoHeight(Long.MIN_VALUE)
            .setMaxVideoHeight(Long.MAX_VALUE)
            .setMinVideoWidth(Long.MIN_VALUE)
            .setMaxVideoWidth(Long.MAX_VALUE);

    /**
     * Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth
     * estimate is unavailable.
     *
     * To reset it, set it to null.
     *
     * <br>
     * <br>
     * If App is using {@link com.kaltura.playkit.Player#updateABRSettings(ABRSettings)}
     * <br>
     * Then Using {@link ABRSettings#setInitialBitrateEstimate(Long)} is unaffected because
     * initial bitrate is only meant at the start of the playback
     * <br>
     *
     * @param initialBitrateEstimate The initial bitrate estimate in bits per second.
     * @return ABRSettings
     */
    public ABRSettings setInitialBitrateEstimate(Long initialBitrateEstimate) {
        this.initialBitrateEstimate = initialBitrateEstimate;
        return this;
    }

    /**
     * Set minVideoBitrate in ABR
     *
     * @param minVideoBitrate - minimum video bitrate in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMinVideoBitrate(long minVideoBitrate) {
        if (minVideoBitrate < 0) {
            minVideoBitrate = Long.MIN_VALUE;
        }
        this.minVideoBitrate = minVideoBitrate;
        return this;
    }

    /**
     * Set maxVideoBitrate in ABR
     *
     * @param maxVideoBitrate - maximum video bitrate in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMaxVideoBitrate(long maxVideoBitrate) {
        if (maxVideoBitrate < 0) {
            maxVideoBitrate = Long.MAX_VALUE;
        }
        this.maxVideoBitrate = maxVideoBitrate;
        return this;
    }

    /**
     * Set maxVideoHeight in ABR
     *
     * @param maxVideoHeight - maximum video height in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMaxVideoHeight(long maxVideoHeight) {
        if (maxVideoHeight < 0) {
            maxVideoHeight = Long.MAX_VALUE;
        }
        this.maxVideoHeight = maxVideoHeight;
        return this;
    }

    /**
     * Set minVideoHeight in ABR
     *
     * @param minVideoHeight - minimum video height in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMinVideoHeight(long minVideoHeight) {
        if (minVideoHeight < 0) {
            minVideoHeight = Long.MIN_VALUE;
        }
        this.minVideoHeight = minVideoHeight;
        return this;
    }

    /**
     * Set maxVideoWidth in ABR
     *
     * @param maxVideoWidth - maximum video width in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMaxVideoWidth(long maxVideoWidth) {
        if (maxVideoWidth < 0) {
            maxVideoWidth = Long.MAX_VALUE;
        }
        this.maxVideoWidth = maxVideoWidth;
        return this;
    }

    /**
     * Set minVideoWidth in ABR
     *
     * @param minVideoWidth - minimum video width in ABR
     * @return - ABRSettings
     */
    public ABRSettings setMinVideoWidth(long minVideoWidth) {
        if (minVideoWidth < 0) {
            minVideoWidth = Long.MIN_VALUE;
        }
        this.minVideoWidth = minVideoWidth;
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

    public Long getMaxVideoHeight() {
        return maxVideoHeight;
    }

    public Long getMinVideoHeight() {
        return minVideoHeight;
    }

    public Long getMaxVideoWidth() {
        return maxVideoWidth;
    }

    public Long getMinVideoWidth() {
        return minVideoWidth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ABRSettings that = (ABRSettings) o;

        if (minVideoBitrate != null ? !minVideoBitrate.equals(that.minVideoBitrate) : that.minVideoBitrate != null)
            return false;
        if (maxVideoBitrate != null ? !maxVideoBitrate.equals(that.maxVideoBitrate) : that.maxVideoBitrate != null)
            return false;
        if (maxVideoHeight != null ? !maxVideoHeight.equals(that.maxVideoHeight) : that.maxVideoHeight != null)
            return false;
        if (minVideoHeight != null ? !minVideoHeight.equals(that.minVideoHeight) : that.minVideoHeight != null)
            return false;
        if (maxVideoWidth != null ? !maxVideoWidth.equals(that.maxVideoWidth) : that.maxVideoWidth != null)
            return false;
        return minVideoWidth != null ? minVideoWidth.equals(that.minVideoWidth) : that.minVideoWidth == null;
    }

    @Override
    public int hashCode() {
        int result = minVideoBitrate != null ? minVideoBitrate.hashCode() : 0;
        result = 31 * result + (maxVideoBitrate != null ? maxVideoBitrate.hashCode() : 0);
        result = 31 * result + (maxVideoHeight != null ? maxVideoHeight.hashCode() : 0);
        result = 31 * result + (minVideoHeight != null ? minVideoHeight.hashCode() : 0);
        result = 31 * result + (maxVideoWidth != null ? maxVideoWidth.hashCode() : 0);
        result = 31 * result + (minVideoWidth != null ? minVideoWidth.hashCode() : 0);
        return result;
    }
}
