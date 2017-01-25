package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.List;

/**
 * @hide
 */

public class KalturaBaseEntryListResponse extends BaseResult {

    public List<KalturaMediaEntry> objects;
    int totalCount;
}
