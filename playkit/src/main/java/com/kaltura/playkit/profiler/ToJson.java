package com.kaltura.playkit.profiler;

import android.graphics.Typeface;
import android.os.Build;

import com.google.gson.JsonArray;
import com.kaltura.android.exoplayer2.text.CaptionStyleCompat;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.Utils.GsonObject;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKMaxVideoSize;
import com.kaltura.playkit.player.PKSubtitlePosition;
import com.kaltura.playkit.player.PlayerSettings;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.vr.VRSettings;

public class ToJson {
    static GsonObject toJson(PKMediaEntry entry) {

        if (entry == null) {
            return null;
        }

        JsonArray sources = new JsonArray();
        if (entry.hasSources()) {
            for (PKMediaSource source : entry.getSources()) {
                sources.add(toJson(source).jsonObject());
            }
        }

        return new GsonObject()
                .add("id", entry.getId())
                .add("duration", entry.getDuration())
                .add("type", String.valueOf(entry.getMediaType()))
                .add("sources", sources);
    }

    static GsonObject toJson(PKMediaSource source) {
        JsonArray drmParams = new JsonArray();
        if (source.hasDrmParams()) {
            for (PKDrmParams params : source.getDrmData()) {
                PKDrmParams.Scheme scheme = params.getScheme();
                if (scheme != null) {
                    drmParams.add(scheme.name());
                }
            }
        }

        return new GsonObject()
                .add("id", source.getId())
                .add("format", source.getMediaFormat().name())
                .add("url", source.getUrl())
                .add("drm", drmParams);
    }

    static GsonObject toJson(ABRSettings s) {
        return new GsonObject()
                .add("initialBitrateEstimate", s.getInitialBitrateEstimate())
                .add("maxVideoBitrate", s.getMaxVideoBitrate())
                .add("minVideoBitrate", s.getMinVideoBitrate());
    }

    static GsonObject toJson(VRSettings s) {
        return new GsonObject()
                .add("flingEnabled", s.isFlingEnabled())
                .add("vrModeEnabled", s.isVrModeEnabled())
                .add("zoomWithPinchEnabled", s.isZoomWithPinchEnabled())
                .add("interactionMode", s.getInteractionMode().name());
    }

    static GsonObject toJson(SubtitleStyleSettings s) {
        return new GsonObject()
                .add("textColor", Integer.toHexString(s.getTextColor()))
                .add("backgroundColor", Integer.toHexString(s.getBackgroundColor()))
                .add("textSizeFraction", s.getTextSizeFraction())
                .add("windowColor", Integer.toHexString(s.getWindowColor()))
                .add("edgeType", edgeTypeName(s.getEdgeType()))
                .add("edgeColor", Integer.toHexString(s.getEdgeColor()))
                .add("typeface", toJson(s.getTypeface()))
                .add("styleName", s.getStyleName())
                .add("position", toJson(s.getSubtitlePosition()));
    }

    private static String edgeTypeName(@CaptionStyleCompat.EdgeType int edgeType) {
        switch (edgeType) {
            case CaptionStyleCompat.EDGE_TYPE_NONE:
                return "none";
            case CaptionStyleCompat.EDGE_TYPE_OUTLINE:
                return "outline";
            case CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW:
                return "dropShadow";
            case CaptionStyleCompat.EDGE_TYPE_RAISED:
                return "raised";
            case CaptionStyleCompat.EDGE_TYPE_DEPRESSED:
                return "depressed";
            default:
                return "unknown";
        }
    }

    static GsonObject toJson(PKSubtitlePosition s) {
        return new GsonObject()
                .add("horizontalPositionPercentage", s.getHorizontalPositionPercentage())
                .add("verticalPositionPercentage", s.getVerticalPositionPercentage())
                .add("overrideInlineCueConfig", s.isOverrideInlineCueConfig())
                .add("horizontalAlignment", s.getSubtitleHorizontalPosition().name());
    }

    static GsonObject toJson(Typeface s) {
        final GsonObject object = new GsonObject()
                .add("bold", s.isBold())
                .add("italic", s.isItalic());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            object.add("weight", s.getWeight());
        }

        return object;
    }

    static GsonObject toJson(PKTrackConfig s) {
        return new GsonObject()
            .add("preferredMode", s.getPreferredMode().name())
            .add("language", s.getTrackLanguage());
    }

    static GsonObject toJson(PKMaxVideoSize s) {
        return new GsonObject()
                .add("width", s.getMaxVideoWidth())
                .add("height", s.getMaxVideoHeight());
    }

    static GsonObject toJson(LoadControlBuffers s) {
        return new GsonObject()
                .addTime("minBufferSec", s.getMinPlayerBufferMs())
                .addTime("maxBufferSec", s.getMaxPlayerBufferMs())
                .addTime("minBufferAfterReBufferSec", s.getMinBufferAfterReBufferMs())
                .addTime("minBufferAfterInteractionSec", s.getMinBufferAfterInteractionMs())
                .addTime("backBufferDurationSec", s.getBackBufferDurationMs())
                .addTime("allowedVideoJoiningTimeSec", s.getAllowedVideoJoiningTimeMs())
                .add("retainBackBufferFromKeyframe", s.getRetainBackBufferFromKeyframe());
    }

    static GsonObject toJson(PlayerSettings settings) {
        return new GsonObject()
                    .add("clearLead", settings.allowClearLead())
                    .add("cea608Captions", settings.cea608CaptionsEnabled())
                    .add("decoderFallback", settings.enableDecoderFallback())
                    .add("abrSettings", toJson(settings.getAbrSettings()))
                    .add("aspectRatioResizeMode", settings.getAspectRatioResizeMode().name())
                    .add("loadControlBuffers", toJson(settings.getLoadControlBuffers()))
                    .add("maxAudioBitrate", settings.getMaxAudioBitrate())
                    .add("maxAudioChannelCount", settings.getMaxAudioChannelCount())
                    .add("maxVideoBitrate", settings.getMaxVideoBitrate())
                    .add("maxVideoSize", toJson(settings.getMaxVideoSize()))
                    .add("preferredAudioTrack", toJson(settings.getPreferredAudioTrackConfig()))
                    .add("preferredMediaFormat", settings.getPreferredMediaFormat().name())
                    .add("preferredTextTrack", toJson(settings.getPreferredTextTrackConfig()))
                    .add("subtitleStyle", toJson(settings.getSubtitleStyleSettings()))
                    .add("vr", toJson(settings.getVRSettings()))
                    .add("adAutoPlayOnResume", settings.isAdAutoPlayOnResume())
                    .add("forceSinglePlayerEngine", settings.isForceSinglePlayerEngine())
                    .add("handleAudioBecomingNoisy", settings.isHandleAudioBecomingNoisyEnabled())
                    .add("secureSurface", settings.isSurfaceSecured())
                    .add("tunneledAudioPlayback", settings.isTunneledAudioPlayback())
                    .add("vrEnabled", settings.isVRPlayerEnabled());
    }

    static GsonObject buildInfoJson() {
        return new GsonObject()
                .add("apiLevel", Build.VERSION.SDK_INT)
                .add("chipset", MediaSupport.DEVICE_CHIPSET)
                .add("brand", Build.BRAND)
                .add("model", Build.MODEL)
                .add("manufacturer", Build.MANUFACTURER)
                .add("device", Build.DEVICE)
                .add("tags", Build.TAGS)
                .add("fingerprint", Build.FINGERPRINT);
    }
}
