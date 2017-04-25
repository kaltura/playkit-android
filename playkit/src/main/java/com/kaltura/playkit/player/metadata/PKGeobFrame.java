package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKGeobFrame implements PKMetadata {

    public final String id;
    public final String mimeType;
    public final String fileName;
    public final String description;
    public final byte[] data;

    public PKGeobFrame(String id, String mimeType, String fileName, String description, byte[] data) {
        this.id = id;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.description = description;
        this.data = data;
    }
}
