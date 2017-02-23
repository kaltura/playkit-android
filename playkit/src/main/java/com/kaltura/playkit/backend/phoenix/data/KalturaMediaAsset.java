package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.BaseResult;

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
