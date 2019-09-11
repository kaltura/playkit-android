package com.kaltura.playkit.player;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.util.MimeTypes;
import com.kaltura.playkit.PKLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PKCodecSupport {

    private static final PKLog log = PKLog.get("PKCodecSupport");

    private static final Set<String> softwareCodecs, hardwareCodecs;

    // This is not a bullet-proof way to detect emulators, but it's good enough for this purpose.
    private static boolean deviceIsEmulator = Build.PRODUCT.equals("sdk") || Build.PRODUCT.startsWith("sdk_") || Build.PRODUCT.endsWith("_sdk");


    static {
        Set<String> hardware = new HashSet<>();
        Set<String> software = new HashSet<>();

        populateCodecSupport(hardware, software);

        softwareCodecs = Collections.unmodifiableSet(software);
        hardwareCodecs = Collections.unmodifiableSet(hardware);
    }

    private static void populateCodecSupport(Set<String> hardware, Set<String> software) {

        ArrayList<MediaCodecInfo> decoders = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();

            for (MediaCodecInfo codecInfo : codecInfos) {
                if (!codecInfo.isEncoder()) {
                    decoders.add(codecInfo);
                }
            }
        } else {
            for (int i = 0, n = MediaCodecList.getCodecCount(); i < n; i++) {
                final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (!codecInfo.isEncoder()) {
                    decoders.add(codecInfo);
                }
            }
        }

        for (MediaCodecInfo codecInfo : decoders) {
            final String name = codecInfo.getName();

            final boolean isHardware;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isHardware = codecInfo.isHardwareAccelerated();
            } else {
                isHardware = !name.startsWith("OMX.google.");
            }

            final List<String> supportedCodecs = Arrays.asList(codecInfo.getSupportedTypes());
            final Set<String> set = isHardware ? hardware : software;
            set.addAll(supportedCodecs);
        }
    }

    public static boolean hasDecoder(String codec, boolean isMimeType, boolean allowSoftware) {

        if (deviceIsEmulator) {
            // Emulators have no hardware codecs, but we still need to play.
            allowSoftware = true;
        }

        final String mimeType = isMimeType ? codec : MimeTypes.getMediaMimeType(codec);
        if (hardwareCodecs.contains(mimeType)) {
            return true;
        }

        return allowSoftware && softwareCodecs.contains(mimeType);
    }
}
