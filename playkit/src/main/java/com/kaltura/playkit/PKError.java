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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PKError {

    @Nullable
    public final String message;
    @Nullable
    public final Throwable exception;
    @NonNull
    public final Enum errorType;
    @NonNull
    public final Enum errorCategory;
    @NonNull
    public final Severity severity;


    public PKError(@NonNull Enum errorType, @Nullable String message, @Nullable Throwable exception) {
        this.errorCategory = PKErrorCategory.UNKNOWN;
        this.errorType = errorType;
        this.severity = Severity.Fatal;
        this.message = message;
        this.exception = exception;
    }

    public PKError(@NonNull Enum errorType, @NonNull Severity severity, @Nullable String message, @Nullable Throwable exception) {
        this.errorCategory = PKErrorCategory.UNKNOWN;
        this.errorType = errorType;
        this.severity = severity;
        this.message = message;
        this.exception = exception;
    }

    public PKError(@NonNull Enum errorCategory, @NonNull Enum errorType, @NonNull Severity severity, @Nullable String message, @Nullable Throwable exception) {
        this.errorCategory = errorCategory;
        this.errorType = errorType;
        this.severity = severity;
        this.message = message;
        this.exception = exception;
    }

    public boolean isFatal() {
        return severity == Severity.Fatal;
    }
    public enum Severity {
        Recoverable,
        Fatal
    }
}
