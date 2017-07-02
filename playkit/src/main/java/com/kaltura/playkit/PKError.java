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
import android.support.annotation.Nullable;

/**
 * Created by anton.afanasiev on 13/06/2017.
 */

public class PKError {

    @Nullable
    public final String message;
    @Nullable
    public final Throwable cause;
    @NonNull
    public final Enum errorType;

    public PKError(@NonNull Enum errorType, @Nullable String message, @Nullable Throwable cause) {
        this.errorType = errorType;
        this.message = message;
        this.cause = cause;
    }

}
