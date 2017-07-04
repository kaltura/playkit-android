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

package com.kaltura.playkit.drm;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public class WidevineNotSupportedException extends RuntimeException {

    public WidevineNotSupportedException(Throwable throwable) {
        super(throwable);
    }

}
