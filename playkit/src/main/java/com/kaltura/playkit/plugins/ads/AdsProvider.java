/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.PKAdInfo;


public interface AdsProvider {
    void start();

    void destroyAdsManager();

    void resume();

    void pause();

    void contentCompleted();

    PKAdInfo getAdInfo();

    boolean isAdDisplayed();

    boolean isAdPaused();

    boolean isAdRequested();

    boolean isAllAdsCompleted();

    boolean isAdError();

    long getDuration();

    long getCurrentPosition();

    void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController);

    void removeAdProviderListener();

    void skipAd();

    enum Event {
        AD_REQUESTED,
        AD_FIRST_PLAY,
        STARTED,
        AD_DISPLAYED_AFTER_CONTENT_PAUSE,
        PAUSED,
        RESUMED,
        COMPLETED,
        FIRST_QUARTILE,
        MIDPOINT,
        THIRD_QUARTILE,
        SKIPPED,
        SKIPPABLE_STATE_CHANGED,
        CLICKED,
        TAPPED,
        ICON_TAPPED,
        AD_BREAK_READY,
        AD_PROGRESS,
        AD_BREAK_STARTED,
        AD_BREAK_ENDED,
        AD_BREAK_IGNORED,
        CUEPOINTS_CHANGED,
        PLAY_HEAD_CHANGED,
        LOADED,
        CONTENT_PAUSE_REQUESTED,
        CONTENT_RESUME_REQUESTED,
        ALL_ADS_COMPLETED,
        AD_LOAD_TIMEOUT_TIMER_STARTED,
        AD_BUFFER_START,
        AD_BUFFER_END,
        AD_PLAYBACK_INFO_UPDATED,
        ERROR
    }

}
