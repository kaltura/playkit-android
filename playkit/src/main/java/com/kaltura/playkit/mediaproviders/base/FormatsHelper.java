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

package com.kaltura.playkit.mediaproviders.base;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.api.base.model.BasePlaybackSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tehilarozin on 13/02/2017.
 */

public class FormatsHelper {

    public enum StreamFormat {
        MpegDash("mpegdash"),
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
//TODO: add supported schemes

    static{
        SupportedFormats = new HashMap<StreamFormat, PKMediaFormat>();
        SupportedFormats.put(StreamFormat.MpegDash, PKMediaFormat.dash);
        SupportedFormats.put(StreamFormat.AppleHttp, PKMediaFormat.hls);
        SupportedFormats.put(StreamFormat.Url, PKMediaFormat.mp4);
        SupportedFormats.put(StreamFormat.UrlDrm, PKMediaFormat.wvm);
    }

    public static Map<StreamFormat, PKMediaFormat> getSupportedFormats() {
        return SupportedFormats;
    }

    // in the future we may need to check the schemes provided in the drm list and decide if scheme is supported.
    public static PKMediaFormat getPKMediaFormat(String format, boolean hasDrm) {

        StreamFormat streamFormat = StreamFormat.byValue(format);
        switch (streamFormat) {
            case MpegDash:
                return PKMediaFormat.dash;
            case Url:
                return hasDrm ? PKMediaFormat.wvm : PKMediaFormat.mp4;
            case AppleHttp:
                return PKMediaFormat.hls;
        }
        return null;
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
