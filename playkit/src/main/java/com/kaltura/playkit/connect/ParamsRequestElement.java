package com.kaltura.playkit.connect;

import com.google.gson.JsonObject;

/**
 * @hide
 */
@Deprecated // will be removed once KalturaOvp... files wil be updated with new design
public interface ParamsRequestElement {
    JsonObject getParams();
    boolean isMultipart();
}
