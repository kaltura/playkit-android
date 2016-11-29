package com.kaltura.playkit.backend.ovp.data;

/**
 * Created by tehilarozin on 31/10/2016.
 */

public class KalturaMediaEntry {

    private int mediaType;
    private String dataUrl;
    private String flavorParamsIds; //consider set as list
    private String id;
    private String name;
    private int msDuration;
    //private int licenseType = -1; //UNKNOWN

    public int getMediaType() {
        return mediaType;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public int[] getFlavorParamsIdsArr(){
        String[] flavors = flavorParamsIds.split(",");

        if(flavors.length > 0){
            int[] flavorsArr = new int[flavors.length];
            for(int i = 0 ; i < flavors.length ; i++){
                flavorsArr[i] = Integer.parseInt(flavors[i]);
            }
            return flavorsArr;
        }
        return new int[0];
    }

    public String getFlavorParamsIds() {
        return flavorParamsIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMsDuration() {
        return msDuration;
    }


}
