package com.kaltura.playkit.backend.base;

import android.support.annotation.StringDef;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.backend.base.data.BasePlaybackSource;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class FormatsHelper {

    @Retention(SOURCE)
    @StringDef(value = {FormatName.MpegDash, FormatName.MpegDashDrm, FormatName.AppleHttp,
            FormatName.Url, FormatName.UrlDrm})
    public @interface FormatName {
        String MpegDash = "mpegdash";
        String MpegDashDrm = "mpegdash+drm";
        String AppleHttp = "applehttp";
        String Url = "url";
        String UrlDrm = "url+drm";
    }

    /**
     * to map BE format name to the matching format element in the {@link PKMediaFormat} enumeration.
     */
    private static final Map<String, PKMediaFormat> SupportedFormats = new HashMap<String, PKMediaFormat>() {{
        put(FormatName.MpegDash, PKMediaFormat.dash_clear);
        put(FormatName.MpegDashDrm, PKMediaFormat.dash_drm);
        put(FormatName.AppleHttp, PKMediaFormat.hls_clear);
        put(FormatName.Url, PKMediaFormat.mp4_clear);
        put(FormatName.UrlDrm, PKMediaFormat.wvm_widevine);
    }};

    public static Map<String, PKMediaFormat> getSupportedFormats() {
        return SupportedFormats;
    }

    // in the future we may need to check the schemes provided in the drm list and decide if scheme is supported.
    public static PKMediaFormat getPKMediaFormat(String format, boolean hasDrm) {
        switch (format) {
            case FormatName.MpegDash:
                return hasDrm ? SupportedFormats.get(FormatName.MpegDashDrm) : SupportedFormats.get(FormatName.MpegDash);
            case FormatName.Url:
                return hasDrm ? SupportedFormats.get(FormatName.UrlDrm) : SupportedFormats.get(FormatName.Url);
            case FormatName.AppleHttp:
                return hasDrm ? null : SupportedFormats.get(FormatName.AppleHttp);
        }
        return null;
    }

    // for future use
    public static PKMediaFormat getPKMediaFormat(String format, String schemes) {
        switch (format) {
            case FormatName.MpegDash:
                return schemes == null ?
                        SupportedFormats.get(FormatName.MpegDash) :
                        schemes.contains(PKDrmParams.Scheme.widevine_cenc.name()) ?
                                SupportedFormats.get(FormatName.MpegDashDrm) :
                                null;

            case FormatName.Url:
                return schemes == null ?
                        SupportedFormats.get(FormatName.Url) :
                        schemes.contains(PKDrmParams.Scheme.widevine_classic.name()) ?
                                SupportedFormats.get(FormatName.UrlDrm) :
                                null;

            case FormatName.AppleHttp:
                return schemes == null ?
                        SupportedFormats.get(FormatName.AppleHttp) :
                        null;
                        /*!schemes.contains(PKDrmParams.Scheme.FAIRPLAY.name()) ?
                                SupportedFormats.get(FormatName.UrlDrm) :
                                null;*/

            default:return null;

        }
    }

    /**
     * checks if the format name from the source parameter has a matching supported {@link PKMediaFormat}
     * element.
     *
     * @param source - playback source item
     * @return - true, if format is valid and supported
     */
    public static boolean validateFormat(BasePlaybackSource source) {
        PKMediaFormat format = getPKMediaFormat(source.getFormat(), source.hasDrmData());
        return format != null;
    }

}