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

public class PKError {

    @Nullable
    public final String message;
    @Nullable
    public final Throwable exception;
    @NonNull
    public final Enum errorType;

    public PKError(@NonNull Enum errorType, @Nullable String message, @Nullable Throwable exception) {
        this.errorType = errorType;
        this.message = message;
        this.exception = exception;
    }

}