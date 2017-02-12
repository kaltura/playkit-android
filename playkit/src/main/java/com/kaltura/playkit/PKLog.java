package com.kaltura.playkit;

import android.util.Log;

import com.kaltura.playkit.plugins.PKPluginAPI;

import java.util.Locale;

/**
 * Logger for PlayKit. 
 * 
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
 *      log.level = PKLog.WARN;
 * }
 * </pre>
 * 
 * 
 * Created by Noam Tamim @ Kaltura on 11/11/2016.
 */




@SuppressWarnings("WeakerAccess")
@PKPluginAPI
public class PKLog {

    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int ASSERT = Log.ASSERT;
    public static final int NOLOG = Integer.MAX_VALUE;

    public final String tag;
    public static int globalLevel = VERBOSE;
    public int level = VERBOSE;
    
    private static String shortenTag(String tag) {
        if (tag.length() > 23) {
            String fixed = String.format(Locale.ENGLISH, "%s_%02x", tag.substring(0, 20), tag.hashCode() & 0xff);
            Log.w("PKLog", "Log tag too long; shortening '" + tag + "' to '" + fixed + "'");
            return fixed;
        }
        return tag;
    }

    private PKLog(String tag) {
        this.tag = shortenTag(tag);
    }

    public static PKLog get(String tag) {
        return new PKLog(tag);
    }


    // VERBOSE

    public void v(String msg) {
        if (level <= VERBOSE) {
            PKLog.v(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (globalLevel <= VERBOSE) {
            Log.v(tag, msg);
        }
    }

    public void v(String msg, Throwable tr) {
        if (level <= VERBOSE) {
            PKLog.v(tag, msg, tr);
        }
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (globalLevel <= VERBOSE) {
            Log.v(tag, msg, tr);
        }
    }



    // DEBUG

    public void d(String msg) {
        if (level <= DEBUG) {
            PKLog.d(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (globalLevel <= DEBUG) {
            Log.d(tag, msg);
        }
    }

    public void d(String msg, Throwable tr) {
        if (level <= DEBUG) {
            PKLog.d(tag, msg, tr);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (globalLevel <= DEBUG) {
            Log.d(tag, msg, tr);
        }
    }



    // INFO

    public void i(String msg) {
        if (level <= INFO) {
            PKLog.i(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (globalLevel <= INFO) {
            Log.i(tag, msg);
        }
    }

    public void i(String msg, Throwable tr) {
        if (level <= INFO) {
            PKLog.i(tag, msg, tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (globalLevel <= INFO) {
            Log.i(tag, msg, tr);
        }
    }



    // WARN

    public void w(String msg) {
        if (level <= WARN) {
            PKLog.w(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (globalLevel <= WARN) {
            Log.w(tag, msg);
        }
    }

    public void w(String msg, Throwable tr) {
        if (level <= WARN) {
            PKLog.w(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (globalLevel <= WARN) {
            Log.w(tag, msg, tr);
        }
    }



    // ERROR

    public void e(String msg) {
        if (level <= ERROR) {
            PKLog.e(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (globalLevel <= ERROR) {
            Log.e(tag, msg);
        }
    }

    public void e(String msg, Throwable tr) {
        if (level <= ERROR) {
            PKLog.e(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (globalLevel <= ERROR) {
            Log.e(tag, msg, tr);
        }
    }
}
