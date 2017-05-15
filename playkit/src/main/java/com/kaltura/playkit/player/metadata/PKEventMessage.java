package com.kaltura.playkit.player.metadata;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKEventMessage implements PKMetadata {

    public final String schemeIdUri;
    public final String value;
    public final long durationMs;
    public final long id;
    public final byte[] messageData;

    public PKEventMessage(String schemeIdUri, String value, long durationMs, long id, byte[] messageData) {
        this.schemeIdUri = schemeIdUri;
        this.value = value;
        this.durationMs = durationMs;
        this.id = id;
        this.messageData = messageData;
    }
}
