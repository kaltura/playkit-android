package com.kaltura.playkit.api.ovp.model;

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;

/**
 * @hide
 */

public class KalturaBaseEntryListResponse extends BaseResult {

    public List<KalturaMediaEntry> objects;
    int totalCount;
}
