package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by zivilan on 15/12/2016.
 */

public class OTTEvent implements PKEvent {
    public final OttEventType type;

    public enum OttEventType
    {Concurrency}

    public OTTEvent(OttEventType type) {
        this.type = type;
    }


    @Override
    public Enum eventType() {
        return this.type;
    }

}
