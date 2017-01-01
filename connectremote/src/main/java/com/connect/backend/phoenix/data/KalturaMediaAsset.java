package com.connect.backend.phoenix.data;

import com.connect.backend.BaseResult;

import java.util.List;

/**
 * Created by tehilarozin on 04/11/2016.
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
