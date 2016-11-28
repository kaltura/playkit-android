package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaBaseEntryListResponse extends BaseResult {

    public List<KalturaMediaEntry> objects;
    int totalCount;
}
