package com.kaltura.playkit.profiler;

import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.util.Locale;

class ProfilerUtil {
    static final float MSEC_MULTIPLIER_FLOAT = 1000f;
    static final String SEPARATOR = "\t";

    private static String toString(Enum e) {
        if (e == null) {
            return "null";
        }
        return e.name();
    }

    static JsonObject toJSON(PKMediaEntry entry) {

        if (entry == null) {
            return null;
        }

        JsonObject json = new JsonObject();

        json.addProperty("id", entry.getId());
        json.addProperty("duration", entry.getDuration());
        json.addProperty("type", toString(entry.getMediaType()));

        if (entry.hasSources()) {
            JsonArray array = new JsonArray();
            for (PKMediaSource source : entry.getSources()) {
                array.add(toJSON(source));
            }
            json.add("sources", array);
        }

        return json;
    }

    private static JsonObject toJSON(PKMediaSource source) {
        JsonObject json = new JsonObject();

        json.addProperty("id", source.getId());
        json.addProperty("format", source.getMediaFormat().name());
        json.addProperty("url", source.getUrl());

        if (source.hasDrmParams()) {
            JsonArray array = new JsonArray();
            for (PKDrmParams params : source.getDrmData()) {
                PKDrmParams.Scheme scheme = params.getScheme();
                if (scheme != null) {
                    array.add(scheme.name());
                }
            }
            json.add("drm", array);
        }

        return json;
    }

    static String field(String name, String value) {
        if (value == null) {
            return null;
        }
        return name + "={" + value + "}";
    }

    static String field(String name, long value) {
        return name + "=" + value;
    }

    static String field(String name, boolean value) {
        return name + "=" + value;
    }

    static String field(String name, float value) {
        return String.format(Locale.US, "%s=%.03f", name, value);
    }

    static String timeField(String name, long value) {
        return value == C.TIME_UNSET ? null : field(name, value / MSEC_MULTIPLIER_FLOAT);
    }

    static String nullable(String name, String value) {
        if (value == null) {
            return name + "=null";
        }

        return field(name, value);
    }

    static String joinFields(String... fields) {
        return TextUtils.join(SEPARATOR, fields);
    }
}
