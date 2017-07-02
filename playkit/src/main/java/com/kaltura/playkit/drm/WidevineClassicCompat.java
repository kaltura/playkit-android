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

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKLog;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

/**
 * @hide
 */

public class WidevineClassicCompat {

    private static final PKLog log = PKLog.get("WidevineClassicCompat");

    /**
     * See description of WidevineClassicDrm.fdToString() for details on this class.
     * This implementation was separated from WidevineClassicDrm to prevent a VerifyError on older devices.
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    static String fdToString23(FileDescriptor fd) {
        try {
            Method method = fd.getClass().getMethod("getInt$");
            Object fdInt = method.invoke(fd);
            if (fdInt instanceof Integer) {
                return "FileDescriptor[" + fdInt + "]";
            }
        } catch (ReflectiveOperationException e) {
            log.e("Error getting FD to string", e);
        }
        return null;
    }
}
