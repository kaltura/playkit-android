package com.connect.backend.ovp;

import com.connect.utils.ErrorElement;

/**
 * Created by tehilarozin on 18/12/2016.
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
