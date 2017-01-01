package com.connect.backend.ovp;

import com.connect.backend.ovp.services.OvpService;
import com.connect.utils.RequestBuilder;
import com.connect.utils.RequestElement;

/**
 * Created by tehilarozin on 08/12/2016.
 */

public class OvpRequestBuilder extends RequestBuilder<OvpRequestBuilder> {

    @Override
    public RequestElement build() {
        addParams(OvpService.getOvpConfigParams());
        return super.build();
    }
}
