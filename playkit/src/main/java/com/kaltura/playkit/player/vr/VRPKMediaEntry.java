package com.kaltura.playkit.player.vr;

import com.kaltura.playkit.PKMediaEntry;

public class VRPKMediaEntry extends PKMediaEntry {

    public VRPKMediaEntry() {}

    public VRPKMediaEntry(PKMediaEntry mediaEntry) {
        if (mediaEntry == null) {
            return;
        }
        if (mediaEntry.getId() != null) {
            this.setId(mediaEntry.getId());
        }
        if (mediaEntry.getName() != null) {
            this.setName(mediaEntry.getName());
        }
        if (mediaEntry.getSources() != null) {
            this.setSources(mediaEntry.getSources());
        }
        this.setDuration(mediaEntry.getDuration());
        if (mediaEntry.getMediaType() != null) {
            this.setMediaType(mediaEntry.getMediaType());
        }
        if (mediaEntry.getExternalSubtitleList() != null) {
            this.setExternalSubtitleList(mediaEntry.getExternalSubtitleList());
        }
        if (mediaEntry.getMetadata() != null) {
            this.setMetadata(mediaEntry.getMetadata());
        }
    }
}
