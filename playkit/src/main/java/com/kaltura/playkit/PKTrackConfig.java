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

package com.kaltura.playkit;

import android.support.annotation.NonNull;

public class PKTrackConfig {

    private String trackLanguage;
    private PKPreferredTrackSelectionMode preferredTrackSelectionMode = PKPreferredTrackSelectionMode.OFF;

    public String getTrackLanguage() {
        return trackLanguage;
    }

    public PKPreferredTrackSelectionMode getPreferredTrackSelectionMode() {
        return preferredTrackSelectionMode;
    }

    public PKTrackConfig setTrackLanguage(String trackLanguage) {
        this.trackLanguage = trackLanguage;
        return this;
    }

    /**
     * Set preferred Track Selection Mode. Can not be null.
     * @param preferredTrackSelectionMode - the preferred mode.
     * @return - {@link PKTrackConfig}
     */
    public PKTrackConfig setPreferredTrackSelectionMode(@NonNull PKPreferredTrackSelectionMode preferredTrackSelectionMode) {
        this.preferredTrackSelectionMode = preferredTrackSelectionMode;
        return this;
    }
}

