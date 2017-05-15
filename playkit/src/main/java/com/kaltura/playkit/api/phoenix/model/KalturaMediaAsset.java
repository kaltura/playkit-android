package com.kaltura.playkit.api.phoenix.model;

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;

/**
 */

public class KalturaMediaAsset extends BaseResult {
    int id;
    int type;
    String name;
    List<KalturaMediaFile> mediaFiles;

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public List<KalturaMediaFile> getFiles() {
        return mediaFiles;
    }

}
