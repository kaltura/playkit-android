package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.phoenix.data.BaseResult;

import java.util.List;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class KalturaEntryList extends BaseResult {

    public List<KalturaMediaEntry> objects;
    int totalCount;
}
