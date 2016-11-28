package com.kaltura.playkit.connect;

import com.google.gson.JsonObject;

/**
 * Created by tehilarozin on 31/10/2016.
 */
@Deprecated // will be removed once KalturaOvp... files wil be updated with new design
public interface ParamsRequestElement {
    JsonObject getParams();
    boolean isMultipart();
}
