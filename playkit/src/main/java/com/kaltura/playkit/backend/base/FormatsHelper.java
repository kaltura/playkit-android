package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.backend.base.data.BasePlaybackSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class FormatsHelper {

    public enum StreamFormat {
        MpegDash("mpegdash"),
        MpegDashDrm("mpegdash+drm"),
        AppleHttp("applehttp"),
        Url("url"),
        UrlDrm("url+drm"),
        Unknown;

        public String formatName = "";

        StreamFormat(){}

        StreamFormat(String name){
            this.formatName = name;
        }

        public static StreamFormat byValue(String value) {
            for(StreamFormat streamFormat : values()){
                if(streamFormat.formatName.equals(value)){
                    return streamFormat;
                }
            }
            return Unknown;
        }
    }

    /**
     * to map BE format name to the matching format element in the {@link PKMediaFormat} enumeration.
     */
    private static final Map<StreamFormat, PKMediaFormat> SupportedFormats;

    static{
        SupportedFormats = new HashMap<StreamFormat, PKMediaFormat>();
        SupportedFormats.put(StreamFormat.MpegDash, PKMediaFormat.dash_clear);
        SupportedFormats.put(StreamFormat.MpegDashDrm, PKMediaFormat.dash_drm);
        SupportedFormats.put(StreamFormat.AppleHttp, PKMediaFormat.hls_clear);
        SupportedFormats.put(StreamFormat.Url, PKMediaFormat.mp4_clear);
        SupportedFormats.put(StreamFormat.UrlDrm, PKMediaFormat.wvm_widevine);
    }

    public static Map<StreamFormat, PKMediaFormat> getSupportedFormats() {
        return SupportedFormats;
    }

    // in the future we may need to check the schemes provided in the drm list and decide if scheme is supported.
    public static PKMediaFormat getPKMediaFormat(String format, boolean hasDrm) {

        StreamFormat streamFormat = StreamFormat.byValue(format);
        switch (streamFormat) {
            case MpegDash:
                return hasDrm ? SupportedFormats.get(StreamFormat.MpegDashDrm) : SupportedFormats.get(StreamFormat.MpegDash);
            case Url:
                return hasDrm ? SupportedFormats.get(StreamFormat.UrlDrm) : SupportedFormats.get(StreamFormat.Url);
            case AppleHttp:
                return hasDrm ? null : SupportedFormats.get(StreamFormat.AppleHttp);
        }
        return null;
    }

    // for future use
    public static PKMediaFormat getPKMediaFormat(String format, String schemes) {
        StreamFormat streamFormat = StreamFormat.byValue(format);
        switch (streamFormat) {
            case MpegDash:
                return schemes == null ?
                        SupportedFormats.get(StreamFormat.MpegDash) :
                        schemes.contains(PKDrmParams.Scheme.widevine_cenc.name()) ?
                                SupportedFormats.get(StreamFormat.MpegDashDrm) :
                                null;

            case Url:
                return schemes == null ?
                        SupportedFormats.get(StreamFormat.Url) :
                        schemes.contains(PKDrmParams.Scheme.widevine_classic.name()) ?
                                SupportedFormats.get(StreamFormat.UrlDrm) :
                                null;

            case AppleHttp:
                return schemes == null ?
                        SupportedFormats.get(StreamFormat.AppleHttp) :
                        null;
                        /*!schemes.contains(PKDrmParams.Scheme.FAIRPLAY.name()) ?
                                SupportedFormats.get(StreamFormat.UrlDrm) :
                                null;*/

            default:
                return null;

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