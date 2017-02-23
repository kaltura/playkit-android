package com.kaltura.playkit.backend.ovp;

import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.RestrictionError;

/**
 * @hide
 */

public class KalturaOvpErrorHelper {

    public static ErrorElement getErrorElement(String code, String message){
        switch (code){
            /*case "SCHEDULED_RESTRICTED":
            case "COUNTRY_RESTRICTED":*/
            case "NoFilesFound":
                return ErrorElement.NotFound.message("Content can't be played due to lack of sources");

            default:
                return new RestrictionError(code+": "+message, RestrictionError.Restriction.NotAllowed);
        }
    }

    public static ErrorElement getErrorElement(String code) {
        return getErrorElement(code, null);
    }
}
