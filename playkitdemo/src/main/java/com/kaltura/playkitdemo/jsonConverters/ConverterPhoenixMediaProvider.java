package com.kaltura.playkitdemo.jsonConverters;

import java.util.List;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class ConverterPhoenixMediaProvider {

    ConverterSessionProvider sessionProvider;
    String assetId;
    String referenceType;
    List<String> formats;


    public String getAssetId() {
        return assetId;
    }

    public List<String> getFormats() {
        return formats;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public ConverterSessionProvider getSessionProvider() {
        return sessionProvider;
    }
}
