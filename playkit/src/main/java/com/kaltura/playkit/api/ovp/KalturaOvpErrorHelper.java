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

package com.kaltura.playkit.api.ovp;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.RestrictionError;

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
