/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ott;

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
