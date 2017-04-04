package com.kaltura.playkit.api.phoenix.model;

import com.kaltura.playkit.api.base.model.BasePlaybackContext;

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
