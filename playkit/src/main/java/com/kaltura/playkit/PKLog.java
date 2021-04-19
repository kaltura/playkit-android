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
import android.util.Log;

import java.util.Locale;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;

/**
 * Logger for PlayKit.
 * <p>
 * Usage:
 *
 * <pre>
 * class MyClass {
 *      private static final PKLog log = PKLog.get("MyClass");
 *
 *      void setName(String name) {
 *          if (name == null) {
 *              log.e("name is null");
 *              return;
 *          }
 *          if (name == "") {
 *              log.w("name is empty");
 *          }
 *          log.d("name is '" + name + "'");
 *          log.v("setting name to " + name);
 *
 *          this.name = name;
 *      }
 *
 *      ...
 *
 *      log.setLevel(PKLog.Level.warn);
 *      PKLog.setGlobalLevel(PKLog.Level.debug);
 * }
 * </pre>
 * <p>
 * <p>
 * Created by Noam Tamim @ Kaltura on 11/11/2016.
 */


@SuppressWarnings("WeakerAccess")
public class PKLog {

    @NonNull
    public final String tag;
    private int level;

    public enum Level {
        verbose(VERBOSE), debug(DEBUG), info(INFO), warn(WARN), error(ERROR), off(Integer.MAX_VALUE);

        final int value;

        Level(int value) {
            this.value = value;
        }
    }

    private static int globalLevel = DEBUG;

    @NonNull
    private static String shortenTag(String tag) {
        if (tag.length() > 23) {
            String fixed = String.format(Locale.ENGLISH, "%s_%02x", tag.substring(0, 20), tag.hashCode() & 0xff);
            Log.w("PKLog", "Log tag too long; shortening '" + tag + "' to '" + fixed + "'");
            return fixed;
        }
        return tag;
    }

    public static void setGlobalLevel(Level level) {
        PKLog.globalLevel = level.value;
    }

    public void setLevel(@NonNull Level level) {
        this.level = level.value;
    }

    private PKLog(@NonNull String tag) {
        this.tag = shortenTag(tag);
    }

    @NonNull public static PKLog get(@NonNull String tag) {
        return new PKLog(tag);
    }

    public boolean isLoggable(Level level) {
        return Log.isLoggable(tag, level.value);
    }

    // VERBOSE

    public void v(String msg) {
        if (level <= VERBOSE && globalLevel <= VERBOSE) {
            Log.v(tag, msg);
        }
    }

    public void v(String msg, Throwable tr) {
        if (level <= VERBOSE && globalLevel <= VERBOSE) {
            Log.v(tag, msg, tr);
        }
    }

    public static void v(String tag, String msg) {
        if (globalLevel <= VERBOSE) {
            Log.v(tag, msg);
        }
    }

    // DEBUG

    public void d(String msg) {
        if (level <= DEBUG && globalLevel <= DEBUG) {
            Log.d(tag, msg);
        }
    }

    public void d(String msg, Throwable tr) {
        if (level <= DEBUG && globalLevel <= DEBUG) {
            Log.d(tag, msg, tr);
        }
    }

    public static void d(String tag, String msg) {
        if (globalLevel <= DEBUG) {
            Log.d(tag, msg);
        }
    }

    // INFO

    public void i(String msg) {
        if (level <= INFO && globalLevel <= INFO) {
            Log.i(tag, msg);
        }
    }

    public void i(String msg, Throwable tr) {
        if (level <= INFO && globalLevel <= INFO) {
            Log.i(tag, msg, tr);
        }
    }

    public static void i(String tag, String msg) {
        if (globalLevel <= INFO) {
            Log.i(tag, msg);
        }
    }


    // WARN

    public void w(String msg) {
        if (level <= WARN && globalLevel <= WARN) {
            Log.w(tag, msg);
        }
    }

    public void w(String msg, Throwable tr) {
        if (level <= WARN && globalLevel <= WARN) {
            Log.w(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        if (globalLevel <= WARN) {
            Log.w(tag, msg);
        }
    }


    // ERROR
    public void e(String msg) {
        if (level <= ERROR && globalLevel <= ERROR) {
            Log.e(tag, msg);
        }
    }

    public void e(String msg, Throwable tr) {
        if (level <= ERROR && globalLevel <= ERROR) {
            Log.e(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (globalLevel <= ERROR) {
            Log.e(tag, msg);
        }
    }
}
