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

public class PKPrivFrame implements PKMetadata {

    public final String id;
    public final String owner;
    public final byte[] privateData;

    public PKPrivFrame(String id, String owner, byte[] privateData) {
        this.id = id;
        this.owner = owner;
        this.privateData = privateData;
    }
}
