package com.kaltura.playkit.api.ovp;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.request.RequestElement;
import com.kaltura.playkit.api.ovp.services.OvpService;

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
