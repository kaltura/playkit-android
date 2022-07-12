package com.kaltura.playkit.player;

import com.kaltura.playkit.utils.Consts;

/**
 * Low Latency configuration for the live medias.
 * <br>
 *    <br>
 * If this config is set then player will use `targetOffsetMs` passed in this configuration.
 * Player takes an account of the bandwidth as well where it tries to avoid re-buffer while
 * approaching to the `targetOffsetMs`.
 * <br>
 *    <br>
 * If app does not pass `PKLowLatencyConfig`
 * then if the media contains `suggestedPresentationDelayMs` OR `target` in `Latency` tag
 * then player will take those value as offset.
 * <br>
 *    <br>
 * If nothing fulfills in the above conditions, the default live offset
 * is {@link com.kaltura.android.exoplayer2.source.dash.DashMediaSource#DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS}.
 * <br>
 *    <br>
 * It's a good practice to have `availabilityTimeOffset` in DASH manifest, it tells that how much
 * earlier the segments are available.
 * <br>
 *    <br>
 * For HLS, `#EXT-X-SERVER-CONTROL` tag should be there. `#EXT-X-PART` should be there for individual
 * segments. `#EXT-X-PRELOAD_HINT` is a good practice to have to indicate the next part.
 */
public class PKLowLatencyConfig {

    private long targetOffsetMs;
    private long minOffsetMs = Consts.TIME_UNSET;
    private long maxOffsetMs = Consts.TIME_UNSET;
    private float minPlaybackSpeed = Consts.DEFAULT_FALLBACK_MIN_PLAYBACK_SPEED;
    private float maxPlaybackSpeed = Consts.DEFAULT_FALLBACK_MAX_PLAYBACK_SPEED;

    public PKLowLatencyConfig(long targetOffsetMs) {
        this.targetOffsetMs = targetOffsetMs;
    }

    /**
     * Reset the Low Latency Configuration
     * Now Player will use the media-defined default values.
     */
    public final static PKLowLatencyConfig UNSET =
            new PKLowLatencyConfig(Consts.TIME_UNSET)
                    .setMaxOffsetMs(Consts.TIME_UNSET)
                    .setMinOffsetMs(Consts.TIME_UNSET)
                    .setMinPlaybackSpeed(Consts.DEFAULT_FALLBACK_MIN_PLAYBACK_SPEED)
                    .setMaxPlaybackSpeed(Consts.DEFAULT_FALLBACK_MAX_PLAYBACK_SPEED);

    public long getTargetOffsetMs() {
        return targetOffsetMs;
    }

    public long getMinOffsetMs() {
        return minOffsetMs;
    }

    public long getMaxOffsetMs() {
        return maxOffsetMs;
    }

    public float getMinPlaybackSpeed() {
        this.minPlaybackSpeed = minPlaybackSpeed > 0 ? minPlaybackSpeed : Consts.DEFAULT_FALLBACK_MIN_PLAYBACK_SPEED;
        return minPlaybackSpeed;
    }

    public float getMaxPlaybackSpeed() {
        this.maxPlaybackSpeed = maxPlaybackSpeed > 0 ? maxPlaybackSpeed : Consts.DEFAULT_FALLBACK_MAX_PLAYBACK_SPEED;
        return maxPlaybackSpeed;
    }

    /**
     * Target live offset, in milliseconds, or {@link Consts#TIME_UNSET} to use the
     * media-defined default.
     * The player will attempt to get close to this live offset during playback if possible.
     */
    public PKLowLatencyConfig setTargetOffsetMs(long targetOffsetMs) {
        this.targetOffsetMs = targetOffsetMs;
        return this;
    }

    /**
     * The minimum allowed live offset, in milliseconds, or {@link Consts#TIME_UNSET}
     * to use the media-defined default.
     * Even when adjusting the offset to current network conditions,
     * the player will not attempt to get below this offset during playback.
     */
    public PKLowLatencyConfig setMinOffsetMs(long minOffsetMs) {
        this.minOffsetMs = minOffsetMs;
        return this;
    }

    /**
     * The maximum allowed live offset, in milliseconds, or {@link Consts#TIME_UNSET}
     * to use the media-defined default.
     * Even when adjusting the offset to current network conditions,
     * the player will not attempt to get above this offset during playback.
     */
    public PKLowLatencyConfig setMaxOffsetMs(long maxOffsetMs) {
        this.maxOffsetMs = maxOffsetMs;
        return this;
    }

    /**
     * Minimum playback speed, or {@link Consts#RATE_UNSET} to use the
     * media-defined default.
     * The minimum playback speed the player can use to fall back
     * when trying to reach the target live offset.
     */
    public PKLowLatencyConfig setMinPlaybackSpeed(float minPlaybackSpeed) {
        this.minPlaybackSpeed = minPlaybackSpeed > 0 ? minPlaybackSpeed : Consts.DEFAULT_FALLBACK_MIN_PLAYBACK_SPEED;
        return this;
    }

    /**
     * Maximum playback speed, or {@link Consts#RATE_UNSET} to use the
     * media-defined default.
     * The maximum playback speed the player can use to catch up
     * when trying to reach the target live offset.
     */
    public PKLowLatencyConfig setMaxPlaybackSpeed(float maxPlaybackSpeed) {
        this.maxPlaybackSpeed = maxPlaybackSpeed > 0 ? maxPlaybackSpeed : Consts.DEFAULT_FALLBACK_MAX_PLAYBACK_SPEED;
        return this;
    }
}
