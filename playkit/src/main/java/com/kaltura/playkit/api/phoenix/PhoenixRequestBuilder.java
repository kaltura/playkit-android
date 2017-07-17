/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

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
