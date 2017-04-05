package com.kaltura.playkit;

import com.google.android.exoplayer2.metadata.Metadata;

import java.util.List;

/**
 * Created by anton.afanasiev on 05/04/2017.
 */

public class PKMetadata {

    private List<Metadata.Entry> metadataEntries;

    public PKMetadata(List<Metadata.Entry> metadataEntries) {
        this.metadataEntries = metadataEntries;
    }

    public List<Metadata.Entry> getMetadataEntries() {
        return metadataEntries;
    }
}
