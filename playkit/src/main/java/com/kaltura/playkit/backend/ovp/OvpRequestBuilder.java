package com.kaltura.playkit.backend.ovp;

import com.kaltura.playkit.backend.ovp.services.OvpService;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestElement;

/**
 * @hide
 */

public class OvpRequestBuilder extends RequestBuilder<OvpRequestBuilder> {

    @Override
    public RequestElement build() {
        addParams(OvpService.getOvpConfigParams());
        return super.build();
    }
}
