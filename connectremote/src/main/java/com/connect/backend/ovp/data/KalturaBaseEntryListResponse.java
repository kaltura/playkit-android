package com.connect.backend.ovp.data;

import com.connect.backend.BaseResult;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaBaseEntryListResponse extends BaseResult {

    public List<KalturaMediaEntry> objects;
    int totalCount;
}
