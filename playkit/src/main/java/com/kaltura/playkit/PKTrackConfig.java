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

import java.util.Locale;

public class PKTrackConfig {
    private static final String NONE = "none";
    private String trackLanguage;
    private Mode preferredMode = Mode.OFF;

    public String getTrackLanguage() {
        if (preferredMode == Mode.OFF) {
            return NONE;
        } else if (preferredMode == Mode.AUTO) {
            trackLanguage  = Locale.getDefault().getLanguage();
            switch (trackLanguage) {
                case "iw":
                    return "he";
                case "ji":
                    return "yi";
                case "in":
                    return "id";
                default:
                    return trackLanguage;
            }
        }
        return trackLanguage;
    }

    public Mode getPreferredMode() {
        return preferredMode;
    }

    public PKTrackConfig setTrackLanguage(String trackLanguage) {
        this.trackLanguage = trackLanguage;
        return this;
    }

    /**
     * Set preferred Track Selection Mode. Can not be null.
     *
     * @param preferredMode - the preferred mode.
     * @return - {@link PKTrackConfig}
     */
    public PKTrackConfig setPreferredMode(@NonNull Mode preferredMode) {
        this.preferredMode = preferredMode;
        return this;
    }

    public enum Mode {
        OFF,
        AUTO,
        EXPLICIT
    }
}

