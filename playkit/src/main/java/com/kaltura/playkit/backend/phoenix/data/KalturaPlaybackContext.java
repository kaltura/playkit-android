package com.kaltura.playkit.backend.phoenix.data;

import com.kaltura.playkit.backend.base.data.BasePlaybackContext;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlaybackContext extends BasePlaybackContext{

    private ArrayList<KalturaPlaybackSource> sources;


    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }



}
