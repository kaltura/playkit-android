package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.List;
/**
 * Created by itayk on 10/01/2017.
 */

public class KalturaMetadataListResponse extends BaseResult {

    public List<KalturaMetadata> objects;
    int totalCount;
}
