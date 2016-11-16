package com.kaltura.playkit.backend.phoenix.data;

import java.util.List;

/**
 * Created by tehilarozin on 04/11/2016.
 */

public class AssetInfo {
    int id;
    int type;
    String name;
    List<MediaFile> mediaFiles;

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public List<MediaFile> getFiles() {
        return mediaFiles;
    }

}
