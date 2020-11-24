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

package com.kaltura.playkit.player;

import androidx.annotation.Nullable;

/**
 * Text track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class TextTrack extends BaseTrack {

    private String label;
    private String language;
    private String mimeType;

    TextTrack(String uniqueId, String language, String label, String mimeType, int selectionFlag) {
        super(uniqueId, selectionFlag, false);
        this.label = label;
        this.language = language;
        this.mimeType = mimeType;
    }

    /**
     * Getter for the track language.
     * Can be null if the language is unknown.
     *
     * @return - the language of the track.
     */
    @Override
    @Nullable public String getLanguage() {
        return language;
    }

    /**
     * Getter for the track label.
     * Can be null if the label is unknown.
     *
     * @return - the label of the track.
     */
    @Nullable public String getLabel() {
        return label;
    }

    /**
     * Getter for the track mimeType.
     *
     * @return - the mimeType of the track.
     */
    public String getMimeType() {
        return mimeType;
    }
}
