package com.kaltura.playkit.backend.ovp;

import com.kaltura.playkit.connect.ErrorElement;

/**
 * @hide
 */

public class KalturaOvpErrorHelper {

    public static ErrorElement getErrorElement(String code, String message){
        switch (code){
            /*case "SCHEDULED_RESTRICTED":
            case "COUNTRY_RESTRICTED":*/
            default:
                return ErrorElement.RestrictionError.message(message);
        }
    }
}
