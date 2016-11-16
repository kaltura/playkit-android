package com.kaltura.playkit.connect;

import java.util.HashMap;

/**
 * Created by tehilarozin on 31/10/2016.
 */
@Deprecated // will be removed once KalturaOvp... files wil be updated with new design
public interface ParamsRequestElement extends RequestElement {

    HashMap<String, String> getParams();
    boolean isMultipart();
}
