package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKUrlLinkFrame implements PKMetadata {

    public final String id;
    public final String url;
    public final String description;

    public PKUrlLinkFrame(String id, String description, String url) {
        this.id = id;
        this.url = url;
        this.description = description;
    }
}
