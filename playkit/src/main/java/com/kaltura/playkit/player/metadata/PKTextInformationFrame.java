package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKTextInformationFrame implements PKMetadata {

    public final String id;
    public final String description;
    public final String value;

    public PKTextInformationFrame(String id, String description, String value) {
        this.id = id;
        this.description = description;
        this.value = value;
    }

}
