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

import android.support.annotation.Nullable;

/**
 * Text track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class TextTrack extends BaseTrack {

    private String label;
    private String language;


    TextTrack(String uniqueId, String language, String label, int selectionFlag) {
        super(uniqueId, selectionFlag, false);
        this.label = label;
        this.language = language;
    }

    /**
     * Getter for the track language.
     * Can be null if the language is unknown.
     * @return - the language of the track.
     */
    public @Nullable String getLanguage() {
        return language;
    }

    /**
     * Getter for the track label.
     * Can be null if the label is unknown.
     * @return - the label of the track.
     */
    public @Nullable String getLabel() {
        return label;
    }

}
