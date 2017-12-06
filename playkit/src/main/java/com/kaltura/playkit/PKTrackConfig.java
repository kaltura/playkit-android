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

public class PKTrackConfig {

    private String trackLanguage;
    private PKPreferredTrackSelectionMode preferredTrackSelectionMode;

    public PKTrackConfig() {
        preferredTrackSelectionMode = PKPreferredTrackSelectionMode.OFF;
    }

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

    public PKTrackConfig setPreferredTrackSelectionMode(PKPreferredTrackSelectionMode preferredTrackSelectionMode) {
        this.preferredTrackSelectionMode = preferredTrackSelectionMode;
        return this;
    }
}

