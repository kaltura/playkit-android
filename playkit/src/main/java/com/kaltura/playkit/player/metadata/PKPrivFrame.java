package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKPrivFrame implements PKMetadata {

    public final String id;
    public final String owner;
    public final byte[] privateData;

    public PKPrivFrame(String id, String owner, byte[] privateData) {
        this.id = id;
        this.owner = owner;
        this.privateData = privateData;
    }
}
