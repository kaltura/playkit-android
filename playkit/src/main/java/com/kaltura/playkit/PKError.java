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
