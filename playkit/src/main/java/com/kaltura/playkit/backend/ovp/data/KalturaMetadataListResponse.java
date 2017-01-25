package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.List;
/**
 * @hide
 */

public class KalturaMetadataListResponse extends BaseResult {

    public List<KalturaMetadata> objects;
    int totalCount;
}
