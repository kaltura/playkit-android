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

public class PKGeobFrame implements PKMetadata {

    public final String id;
    public final String mimeType;
    public final String fileName;
    public final String description;
    public final byte[] data;

    public PKGeobFrame(String id, String mimeType, String fileName, String description, byte[] data) {
        this.id = id;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.description = description;
        this.data = data;
    }
}
