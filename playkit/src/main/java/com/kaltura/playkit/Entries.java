package com.kaltura.playkit;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 26/10/2016.
 */

public class Entries {
    @SerializedName("entries")
    ArrayList<MediaEntry> mediaEntries;

    public ArrayList<MediaEntry> getMediaEntries() {
        return mediaEntries;
    }

    public void setMediaEntries(ArrayList<MediaEntry> mediaEntries) {
        this.mediaEntries = mediaEntries;
    }

    public MediaEntry get(String id) {
        for (MediaEntry mediaEntry : mediaEntries){
            if(mediaEntry.getId().equals(id)){
                return mediaEntry;
            }
        }
        return null;
    }

    public MediaEntry get(int index) {
        return mediaEntries.get(index);
    }
}
