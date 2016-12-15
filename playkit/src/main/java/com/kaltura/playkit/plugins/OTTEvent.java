package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by zivilan on 15/12/2016.
 */

public class OttEvent implements PKEvent {
    public final OttEvent.OttEventType type;

    public enum OttEventType
    {Concurrency}

    public OttEvent(OttEvent.OttEventType type) {
        this.type = type;
    }


    @Override
    public Enum eventType() {
        return this.type;
    }

}
