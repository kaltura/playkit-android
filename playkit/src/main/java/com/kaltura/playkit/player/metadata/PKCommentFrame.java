package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKCommentFrame implements PKMetadata {

    public final String id;
    public final String language;
    public final String description;
    public final String text;

    public PKCommentFrame(String id, String language, String description, String text) {
        this.id = id;
        this.language = language;
        this.description = description;
        this.text = text;
    }
}
