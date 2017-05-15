package com.kaltura.playkit.api.phoenix;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.request.RequestElement;
import com.kaltura.playkit.api.phoenix.services.PhoenixService;

/**
 */

public class PhoenixRequestBuilder extends RequestBuilder<PhoenixRequestBuilder> {

    @Override
    public RequestElement build() {
        addParams(PhoenixService.getPhoenixConfigParams());
        return super.build();
    }
}
