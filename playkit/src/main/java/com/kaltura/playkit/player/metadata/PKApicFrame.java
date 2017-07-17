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

public class PKApicFrame implements PKMetadata {

    public final String id;
    public final String mimeType;
    public final String description;
    public final int pictureType;
    public final byte[] pictureData;

    public PKApicFrame(String id, String mimeType, String description, int pictureType, byte[] pictureData) {
        this.id = id;
        this.mimeType = mimeType;
        this.description = description;
        this.pictureType = pictureType;
        this.pictureData = pictureData;
    }
}
