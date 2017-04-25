package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKBinaryFrame implements PKMetadata {

    public final String id;
    public final byte[] data;

    public PKBinaryFrame(String id, byte[] data) {
        this.id = id;
        this.data = data;
    }
}
