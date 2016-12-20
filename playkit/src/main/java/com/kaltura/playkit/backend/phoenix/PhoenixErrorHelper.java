package com.kaltura.playkit.backend.phoenix;

import com.kaltura.playkit.connect.ErrorElement;

/**
 * Created by tehilarozin on 18/12/2016.
 */

public class PhoenixErrorHelper {

    /**
     * in case specific error codes should be parsed to predefined errors.
     * @param code
     * @param message
     * @return
     */
    public static ErrorElement getErrorElement(String code, String message){
        switch (code){
            case "500016":
                return ErrorElement.SessionError.message("session token has been expired");

            default:
                return ErrorElement.GeneralError.message(message);
        }
    }
}
