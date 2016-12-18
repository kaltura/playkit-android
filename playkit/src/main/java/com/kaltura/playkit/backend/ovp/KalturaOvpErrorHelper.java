package com.kaltura.playkit.backend.ovp;

import com.kaltura.playkit.connect.ErrorElement;

/**
 * Created by tehilarozin on 18/12/2016.
 */

public class KalturaOvpErrorHelper {

    public static ErrorElement getErrorElement(String code, String message){
        switch (code){
            case "SCHEDULED_RESTRICTED":
                return ErrorElement.RestrictionError.message(message);

            default:
                return ErrorElement.GeneralError.message(message);
        }
    }
}
