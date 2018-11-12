package com.kaltura.playkit.player;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;
import com.kaltura.playkit.PKLog;

import java.util.HashMap;

public class PKCodecSupport {

    private static final PKLog log = PKLog.get("PKCodecSupport");
    private static HashMap<TrackType, HashMap<String, Boolean>> cache = new HashMap<>();

    enum TrackType {
        UNKNOWN, VIDEO, AUDIO, TEXT
    }

    static {
        cache.put(TrackType.VIDEO, new HashMap<String, Boolean>());
        cache.put(TrackType.AUDIO, new HashMap<String, Boolean>());
    }

    private static boolean isCodecSupportedInternal(String codec, TrackType type) {
        String mimeType = (type == TrackType.AUDIO) ? MimeTypes.getAudioMediaMimeType(codec) :
                                               MimeTypes.getVideoMediaMimeType(codec);

        for (int i = 0, codecCount = MediaCodecList.getCodecCount(); i < codecCount; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            for (String supportedType : codecInfo.getSupportedTypes()) {
                if (supportedType.equalsIgnoreCase(mimeType)) {
                    // TODO: also verify the attributes
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCodecSupported(@NonNull String codec, @NonNull TrackType type) {
        if (type == TrackType.UNKNOWN) {
            return false;
        }
        final HashMap<String, Boolean> typeCache = cache.get(type);
        if (typeCache != null && typeCache.containsKey(codec)) {
            final Boolean cachedValue = typeCache.get(codec);
            if (cachedValue != null) {
                return cachedValue;
            }
        }

        final boolean sup = isCodecSupportedInternal(codec, type);
        typeCache.put(codec, sup);
        return sup;
    }

    public static boolean isFormatSupported(@NonNull Format format, @Nullable TrackType type) {

        if (type == TrackType.TEXT) {
            return true;    // always supported
        }

        if (format.codecs == null) {
            log.w("isFormatSupported: codecs==null, assuming supported");
            return true;
        }

        if (type == null) {
            // type==null: HLS muxed track with a <video,audio> tuple
            final String[] split = TextUtils.split(format.codecs, ",");
            boolean result = true;
            switch (split.length) {
                case 0: return false;
                case 2: result = isCodecSupported(split[1], TrackType.AUDIO);
                // fallthrough
                case 1: result &= isCodecSupported(split[0], TrackType.VIDEO);
            }
            return result;

        } else {
            return isCodecSupported(format.codecs, type);
        }
    }
}
