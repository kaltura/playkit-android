package com.kaltura.playkit;

import com.google.android.exoplayer2.metadata.emsg.EventMessage;

/**
 * Created by anton.afanasiev on 03/04/2017.
 */

public class PKEmsgMetadata {

    private EventMessage eventMessage = null;

    public PKEmsgMetadata setEventMessage(EventMessage eventMessage) {
        this.eventMessage = eventMessage;
        return this;
    }

    public boolean hasEventMessage() {
        return eventMessage != null;
    }

    public EventMessage getEventMessage() {
        return eventMessage;
    }

    public boolean hasMetadata() {
        return eventMessage != null;
    }
}
