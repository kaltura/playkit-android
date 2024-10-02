package com.kaltura.androidx.media3.exoplayer.video;

import static android.view.Display.DEFAULT_DISPLAY;

import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.ADAPTIVE_NOT_SEAMLESS;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.ADAPTIVE_SEAMLESS;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.DECODER_SUPPORT_FALLBACK;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.DECODER_SUPPORT_FALLBACK_MIMETYPE;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.DECODER_SUPPORT_PRIMARY;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.HARDWARE_ACCELERATION_NOT_SUPPORTED;
import static com.kaltura.androidx.media3.exoplayer.RendererCapabilities.HARDWARE_ACCELERATION_SUPPORTED;
import static com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getAlternativeCodecMimeType;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.kaltura.androidx.media3.common.C;
import com.kaltura.androidx.media3.common.DrmInitData;
import com.kaltura.androidx.media3.common.Format;
import com.kaltura.androidx.media3.common.MimeTypes;
import com.kaltura.androidx.media3.common.util.UnstableApi;
import com.kaltura.androidx.media3.common.util.Util;
import com.kaltura.androidx.media3.exoplayer.RendererCapabilities;
import com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import com.kaltura.playkit.PKLog;

import java.util.List;

@UnstableApi
public class MediaCodecSupportFormatHelper {

    private static final PKLog log = PKLog.get("MediaCodecSupport");
    private final Context context;

    public MediaCodecSupportFormatHelper(Context context) {
        this.context = context;
    }

    public int supportsFormat(MediaCodecSelector mediaCodecSelector, Format format) throws MediaCodecUtil.DecoderQueryException {
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isVideo(mimeType)) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
        }
        @Nullable DrmInitData drmInitData = format.drmInitData;
        // Assume encrypted content requires secure decoders.
        boolean requiresSecureDecryption = drmInitData != null;
        List<MediaCodecInfo> decoderInfos = getDecoderInfos(context, mediaCodecSelector, format, requiresSecureDecryption,
                /* requiresTunnelingDecoder= */ false);
        if (requiresSecureDecryption && decoderInfos.isEmpty()) {
            // No secure decoders are available. Fall back to non-secure decoders.
            decoderInfos = getDecoderInfos(context, mediaCodecSelector, format,
                    /* requiresSecureDecoder= */ false,
                    /* requiresTunnelingDecoder= */ false);
        }
        if (decoderInfos.isEmpty()) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE);
        }
        if (!supportsFormatDrm(format)) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_DRM);
        }
        // Check whether the first decoder supports the format. This is the preferred decoder for the
        // format's MIME type, according to the MediaCodecSelector.
        MediaCodecInfo decoderInfo = decoderInfos.get(0);
        boolean isFormatSupported = decoderInfo.isFormatSupported(format);
        log.d("Preferred decoder: " + decoderInfo.name + ", isFormatSupported=" + isFormatSupported);

        // =====
        // KUX-489
        // https://github.com/google/ExoPlayer/issues/10898
        // Check whether the first decoder supports the format's resolution and frame rate.
        if (!isFormatSupported && Util.SDK_INT >= 29) {
            if (decoderInfo.capabilities != null) {
                log.d("Check resolution and frame rate support for " + decoderInfo.name);
                log.d("Format: " + format);
                isFormatSupported = decoderInfo.capabilities.getVideoCapabilities()
                        .areSizeAndRateSupported(format.width, format.height, format.frameRate);
                log.d("VideoCapabilities areSizeAndRateSupported=" + isFormatSupported);
            }
        }
        // =====

        boolean isPreferredDecoder = true;
        if (!isFormatSupported) {
            // Check whether any of the other decoders support the format.
            for (int i = 1; i < decoderInfos.size(); i++) {
                MediaCodecInfo otherDecoderInfo = decoderInfos.get(i);
                if (otherDecoderInfo.isFormatSupported(format)) {
                    decoderInfo = otherDecoderInfo;
                    isFormatSupported = true;
                    isPreferredDecoder = false;
                    break;
                }
            }
        }
        @C.FormatSupport int formatSupport = isFormatSupported ? C.FORMAT_HANDLED : C.FORMAT_EXCEEDS_CAPABILITIES;
        @RendererCapabilities.AdaptiveSupport int adaptiveSupport = decoderInfo.isSeamlessAdaptationSupported(format) ? ADAPTIVE_SEAMLESS : ADAPTIVE_NOT_SEAMLESS;
        @RendererCapabilities.HardwareAccelerationSupport int hardwareAccelerationSupport = decoderInfo.hardwareAccelerated ? HARDWARE_ACCELERATION_SUPPORTED : HARDWARE_ACCELERATION_NOT_SUPPORTED;
        @RendererCapabilities.DecoderSupport int decoderSupport = isPreferredDecoder ? DECODER_SUPPORT_PRIMARY : DECODER_SUPPORT_FALLBACK;

        if (Util.SDK_INT >= 26 && MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && !Api26.doesDisplaySupportDolbyVision(context)) {
            decoderSupport = DECODER_SUPPORT_FALLBACK_MIMETYPE;
        }

        @RendererCapabilities.TunnelingSupport int tunnelingSupport = RendererCapabilities.TUNNELING_NOT_SUPPORTED;
        if (isFormatSupported) {
            List<MediaCodecInfo> tunnelingDecoderInfos = getDecoderInfos(context, mediaCodecSelector, format, requiresSecureDecryption,
                    /* requiresTunnelingDecoder= */ true);
            if (!tunnelingDecoderInfos.isEmpty()) {
                MediaCodecInfo tunnelingDecoderInfo = MediaCodecUtil.getDecoderInfosSortedByFormatSupport(tunnelingDecoderInfos, format).get(0);
                if (tunnelingDecoderInfo.isFormatSupported(format) && tunnelingDecoderInfo.isSeamlessAdaptationSupported(format)) {
                    tunnelingSupport = RendererCapabilities.TUNNELING_SUPPORTED;
                }
            }
        }

        return RendererCapabilities.create(formatSupport, adaptiveSupport, tunnelingSupport, hardwareAccelerationSupport, decoderSupport);
    }

    /**
     * Returns whether this renderer supports the given {@link Format Format's} DRM scheme.
     */
    private static boolean supportsFormatDrm(Format format) {
        return format.cryptoType == C.CRYPTO_TYPE_NONE || format.cryptoType == C.CRYPTO_TYPE_FRAMEWORK;
    }

    /**
     * Returns a list of decoders that can decode media in the specified format, in the priority order
     * specified by the {@link MediaCodecSelector}. Note that since the {@link MediaCodecSelector}
     * only has access to {@link Format#sampleMimeType}, the list is not ordered to account for
     * whether each decoder supports the details of the format (e.g., taking into account the format's
     * profile, level, resolution and so on). {@link
     * MediaCodecUtil#getDecoderInfosSortedByFormatSupport} can be used to further sort the list into
     * an order where decoders that fully support the format come first.
     *
     * @param mediaCodecSelector       The decoder selector.
     * @param format                   The {@link Format} for which a decoder is required.
     * @param requiresSecureDecoder    Whether a secure decoder is required.
     * @param requiresTunnelingDecoder Whether a tunneling decoder is required.
     * @return A list of {@link MediaCodecInfo}s corresponding to decoders. May be empty.
     * @throws MediaCodecUtil.DecoderQueryException Thrown if there was an error querying decoders.
     */
    private static List<MediaCodecInfo> getDecoderInfos(Context context, MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
        if (format.sampleMimeType == null) {
            return ImmutableList.of();
        }
        if (Util.SDK_INT >= 26 && MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && !Api26.doesDisplaySupportDolbyVision(context)) {
            List<MediaCodecInfo> alternativeDecoderInfos = getAlternativeDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
            if (!alternativeDecoderInfos.isEmpty()) {
                return alternativeDecoderInfos;
            }
        }
        return getDecoderInfosSoftMatch(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
    }

    public static List<MediaCodecInfo> getAlternativeDecoderInfos(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
        @Nullable String alternativeMimeType = getAlternativeCodecMimeType(format);
        if (alternativeMimeType == null) {
            return ImmutableList.of();
        }
        return mediaCodecSelector.getDecoderInfos(alternativeMimeType, requiresSecureDecoder, requiresTunnelingDecoder);
    }

    public static List<MediaCodecInfo> getDecoderInfosSoftMatch(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
        List<MediaCodecInfo> decoderInfos = mediaCodecSelector.getDecoderInfos(format.sampleMimeType, requiresSecureDecoder, requiresTunnelingDecoder);
        List<MediaCodecInfo> alternativeDecoderInfos = getAlternativeDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
        return ImmutableList.<MediaCodecInfo>builder().addAll(decoderInfos).addAll(alternativeDecoderInfos).build();
    }

    @RequiresApi(26)
    private static final class Api26 {
        @DoNotInline
        public static boolean doesDisplaySupportDolbyVision(Context context) {
            boolean supportsDolbyVision = false;
            DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display display = (displayManager != null) ? displayManager.getDisplay(DEFAULT_DISPLAY) : null;
            if (display != null && display.isHdr()) {
                int[] supportedHdrTypes = display.getHdrCapabilities().getSupportedHdrTypes();
                for (int hdrType : supportedHdrTypes) {
                    if (hdrType == Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) {
                        supportsDolbyVision = true;
                        break;
                    }
                }
            }
            return supportsDolbyVision;
        }
    }
}
