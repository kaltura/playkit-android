package com.kaltura.playkit.backend.ovp;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static com.kaltura.playkit.backend.ovp.APIDefines.ResponseProfileType.ExcludeFields;
import static com.kaltura.playkit.backend.ovp.APIDefines.ResponseProfileType.IncludeFields;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 20/11/2016.
 */

public class APIDefines {

    /**
     * optional value for "responseProfile.type" property. Defines how to use the fields provided on responseProfile.fields property.
     * IncludeFields - Indicates that the response should <b>only</b> contain responseProfile.fields.
     * ExcludeFields - Indicates that the response should <b>not</b> contain responseProfile.fields.
     */
    @Retention(SOURCE)
    @IntDef(value = {
            IncludeFields,
            ExcludeFields
    })

    public @interface ResponseProfileType {
        int IncludeFields = 1;
        int ExcludeFields = 2;
    }

}
