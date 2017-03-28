package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 22/02/2017.
 */
public class PKMediaConfig {
    private long startPosition = 0;
    private PKMediaEntry mediaEntry;
    //If set to true, will allow crossprotocol redirection.
    private boolean allowCrossProtocolRedirect = false;

    /**
     * Getter for start position. Default is 0.
     * Note, that start position is in seconds.
     *
     * @return - the start position
     */
    public long getStartPosition() {
        return startPosition;
    }

    /**
     * Setter for start position.
     * Note, that start position is in seconds.
     *
     * @param startPosition - the position from which the media should start.
     * @return - the config object.
     */
    public PKMediaConfig setStartPosition(long startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    public PKMediaConfig setMediaEntry(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        return this;
    }

    /**
     * Setting this to true, will allow cross protocol redirection from http to https and
     * vice versa. Note, by default it will be alwayas set to false.
     * @param allowCrossProtocolRedirect - should allow cross protocol redirect
     * @return - the config object.
     */
    public PKMediaConfig setAllowCrossProtocolRedirect(boolean allowCrossProtocolRedirect) {
        this.allowCrossProtocolRedirect = allowCrossProtocolRedirect;
        return this;
    }

    /**
     * Getter for allowCrossProtocolRedirect field.
     * @return - allowCrossProtocolRedirect.
     */
    public boolean isAllowCrossProtocolRedirect() {
        return allowCrossProtocolRedirect;
    }
}
