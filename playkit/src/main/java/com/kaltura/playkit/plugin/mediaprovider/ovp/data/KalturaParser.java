package com.kaltura.playkit.plugin.mediaprovider.ovp.data;

import com.kaltura.playkit.MediaEntry;
import com.kaltura.playkit.MediaSource;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaParser {

    public void parseMediaEntry(ArrayList<KalturaMediaEntry> mediaEntries, KalturaEntryContextDataResult contextData){

        MediaEntry mediaEntry = new MediaEntry();

        for(KalturaMediaEntry entry : mediaEntries){
            ArrayList<FlavorAsset> supportedFlavors = new ArrayList<>();

            if(contextData != null){
                int[] flavorParamsIdsArr = entry.getFlavorParamsIdsArr();
                if(flavorParamsIdsArr != null && flavorParamsIdsArr.length > 0 ) {
                    String flavorIds = "";

                    for(int i = 0 ; i <flavorParamsIdsArr.length; i++){
                        FlavorAsset flavor;
                        if((flavor = contextData.containsFlavor(flavorParamsIdsArr[i])) != null){
                            flavorIds += (flavorIds.length() > 0 ? "," : "") + flavor.getId();
                            supportedFlavors.add(flavor);
                        }
                    }
                    if(flavorIds.length() > 0){
                        MediaSource mediaSource = new MediaSource();
                        mediaSource.setId(entry.getId());
                        mediaSource.setMimeType(entry.);
                    }
                }
            }
        }

        mediaEntry.setSources();
    }


    public void getMediaSource(String baseUrl, String flavorIds)
}
