package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKApicFrame implements PKMetadata {

    public final String id;
    public final String mimeType;
    public final String description;
    public final int pictureType;
    public final byte[] pictureData;

    public PKApicFrame(String id, String mimeType, String description, int pictureType, byte[] pictureData) {
        this.id = id;
        this.mimeType = mimeType;
        this.description = description;
        this.pictureType = pictureType;
        this.pictureData = pictureData;
    }
}
