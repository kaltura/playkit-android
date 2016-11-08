package com.kaltura.playkit;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 26/10/2016.
 */

public class Entries {
    @SerializedName("entries")
    ArrayList<PKMediaEntry> mediaEntries;

    public ArrayList<PKMediaEntry> getMediaEntries() {
        return mediaEntries;
    }

    public void setMediaEntries(ArrayList<PKMediaEntry> mediaEntries) {
        this.mediaEntries = mediaEntries;
    }

    public PKMediaEntry get(String id) {
        for (PKMediaEntry mediaEntry : mediaEntries){
            if(mediaEntry.getId().equals(id)){
                return mediaEntry;
            }
        }
        return null;
    }

    public PKMediaEntry get(int index) {
        return mediaEntries.get(index);
    }
}
