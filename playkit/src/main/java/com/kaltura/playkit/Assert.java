package com.kaltura.playkit;

import android.text.TextUtils;

/**
 * @hide
 */

public class Assert {
    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkState(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    public static void failState(Object errorMessage) {
        throw new IllegalStateException(String.valueOf(errorMessage));
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static String checkNotEmpty(String string, Object errorMessage) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
        return string;
    }

    public static void shouldNeverHappen() {
        failState("Should never happen");
    }
}
