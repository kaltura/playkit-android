package com.kaltura.playkit.plugins.mediaproviders.phoenix.data;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 04/11/2016.
 */

public class AssetInfo {
    int id;
    int type;
    ArrayList<MediaFile> files;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ArrayList<MediaFile> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<MediaFile> files) {
        this.files = files;
    }
}
