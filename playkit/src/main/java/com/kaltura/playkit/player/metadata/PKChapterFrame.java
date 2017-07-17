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

package com.kaltura.playkit.player.metadata;

import java.util.List;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class PKChapterFrame implements PKMetadata {

    public final String id;
    public final String chapterId;
    public final int startTimeMs;
    public final int endTimeMs;


    public final long startOffset;
    public final long endOffset;
    public final List<PKMetadata> subFrames;

    public PKChapterFrame(String id, String chapterId, int startTimeMs, int endTimeMs, long startOffset, long endOffset, List<PKMetadata> subFrames) {
        this.id = id;
        this.chapterId = chapterId;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.subFrames = subFrames;
    }
}

