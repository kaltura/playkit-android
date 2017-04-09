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

