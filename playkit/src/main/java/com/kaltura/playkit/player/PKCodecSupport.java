package com.kaltura.playkit.player;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kaltura.androidx.media3.common.MimeTypes;
import com.kaltura.playkit.PKLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PKCodecSupport {

    private static final PKLog log = PKLog.get("PKCodecSupport");

    private static final Set<String> softwareCodecs, hardwareCodecs;

    // Video mime type for HEVC
    private static final String HEVC_MIME_TYPE = MimeTypes.VIDEO_H265;

    // This is not a bullet-proof way to detect emulators, but it's good enough for this purpose.
    private static boolean deviceIsEmulator = Build.PRODUCT.equals("sdk") || Build.PRODUCT.startsWith("sdk_") || Build.PRODUCT.endsWith("_sdk");


    static {
        Set<String> hardwareCodecSet = new HashSet<>();
        Set<String> softwareCodeSet = new HashSet<>();

        populateCodecSupport(hardwareCodecSet, softwareCodeSet);

        softwareCodecs = Collections.unmodifiableSet(softwareCodeSet);
        hardwareCodecs = Collections.unmodifiableSet(hardwareCodecSet);
    }

    private static void populateCodecSupport(Set<String> hardwareCodecSet, Set<String> softwareCodeSet) {

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
                isHardware = isHardwareCodecV29(codecInfo);
            } else {
                isHardware = !isSoftwareOnly(name);
            }

            final List<String> supportedCodecs = Arrays.asList(codecInfo.getSupportedTypes());
            final Set<String> set = isHardware ? hardwareCodecSet : softwareCodeSet;
            set.addAll(supportedCodecs);
        }
    }

    private static boolean isSoftwareOnly(String codecName) {
        String codecNameLowerCase = codecName.toLowerCase(Locale.US);
        if (codecNameLowerCase.startsWith("arc.")) { // App Runtime for Chrome (ARC) codecs
            return false;
        }
        return codecNameLowerCase.startsWith("omx.google.")
                || codecNameLowerCase.startsWith("omx.ffmpeg.")
                || (codecNameLowerCase.startsWith("omx.sec.") && codecNameLowerCase.contains(".sw."))
                || codecNameLowerCase.equals("omx.qcom.video.decoder.hevcswvdec")
                || codecNameLowerCase.startsWith("c2.android.")
                || codecNameLowerCase.startsWith("c2.google.")
                || (!codecNameLowerCase.startsWith("omx.") && !codecNameLowerCase.startsWith("c2."));
    }

    @TargetApi(Build.VERSION_CODES.Q)
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("NewApi")
    private static boolean isHardwareCodecV29(MediaCodecInfo codecInfo) {
        return codecInfo.isHardwareAccelerated();
    }

    static boolean hasDecoder(String codec, boolean isMimeType, boolean allowSoftware) {

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

    static boolean isSoftwareHevcSupported() {
        return softwareCodecs.contains(HEVC_MIME_TYPE);
    }

    static boolean isHardwareHevcSupported() {
        return hardwareCodecs.contains(HEVC_MIME_TYPE);
    }
}