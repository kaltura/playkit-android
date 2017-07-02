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

package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKCommentFrame implements PKMetadata {

    public final String id;
    public final String language;
    public final String description;
    public final String text;

    public PKCommentFrame(String id, String language, String description, String text) {
        this.id = id;
        this.language = language;
        this.description = description;
        this.text = text;
    }
}
