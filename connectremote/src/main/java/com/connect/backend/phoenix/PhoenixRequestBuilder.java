package com.connect.backend.phoenix;

import com.connect.backend.phoenix.services.PhoenixService;
import com.connect.utils.RequestBuilder;
import com.connect.utils.RequestElement;

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
