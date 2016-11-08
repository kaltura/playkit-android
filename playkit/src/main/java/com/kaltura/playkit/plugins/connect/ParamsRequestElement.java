package com.kaltura.playkit.plugins.connect;

import java.util.HashMap;

/**
 * Created by tehilarozin on 31/10/2016.
 */

public interface ParamsRequestElement extends RequestElement {

    HashMap<String, String> getParams();
    boolean isMultipart();
}
