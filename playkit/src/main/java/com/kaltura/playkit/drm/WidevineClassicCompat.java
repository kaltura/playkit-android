package com.kaltura.playkit.drm;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKLog;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

/**
 * Created by gilad.nadav on 27/04/2017.
 */

public class WidevineClassicCompat {

    private static final PKLog log = PKLog.get("WidevineClassicCompat");

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
