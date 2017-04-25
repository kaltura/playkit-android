package com.kaltura.playkit.api.ovp.model;

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;
/**
 * @hide
 */

public class KalturaMetadataListResponse extends BaseResult {

    public List<KalturaMetadata> objects;
    int totalCount;
}
