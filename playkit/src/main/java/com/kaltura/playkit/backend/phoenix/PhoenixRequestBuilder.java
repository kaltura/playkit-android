package com.kaltura.playkit.backend.phoenix;

import com.kaltura.playkit.backend.phoenix.services.PhoenixService;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestElement;

/**
 * Created by tehilarozin on 08/12/2016.
 */

public class PhoenixRequestBuilder extends RequestBuilder<PhoenixRequestBuilder> {

    @Override
    public RequestElement build() {
        addParams(PhoenixService.getPhoenixConfigParams());
        return super.build();
    }
}
