package com.kaltura.playkit.player;

import com.kaltura.playkit.PKAudioCodec;

import java.util.ArrayList;
import java.util.List;

public class AudioCodecSettings {
    private List<PKAudioCodec> audioCodecPriorityList = new ArrayList<>();
    private boolean allowAudioMixedMimeTypes = false;

    public AudioCodecSettings() {
        audioCodecPriorityList = getDefaultCodecsPriorityList();
    }

    public AudioCodecSettings(List<PKAudioCodec> audioCodecPriorityList, boolean allowAudioMixedMimeTypes) {
        if (audioCodecPriorityList != null || audioCodecPriorityList.isEmpty()) {
            this.audioCodecPriorityList = audioCodecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowAudioMixedMimeTypes = allowAudioMixedMimeTypes;
    }

    public List<PKAudioCodec> getAudioCodecPriorityList() {
        return audioCodecPriorityList;
    }

    public boolean getAllowAudioMixedMimeTypes() {
        return allowAudioMixedMimeTypes;
    }

    private List<PKAudioCodec> getDefaultCodecsPriorityList() {
        if (audioCodecPriorityList == null) {
            audioCodecPriorityList = new ArrayList<>();
        }
        audioCodecPriorityList.add(PKAudioCodec.E_AC3);

        audioCodecPriorityList.add(PKAudioCodec.AC3);

        audioCodecPriorityList.add(PKAudioCodec.OPUS);

        audioCodecPriorityList.add(PKAudioCodec.AAC);

        return audioCodecPriorityList;
    }

    public AudioCodecSettings setAudioCodecPriorityList(List<PKAudioCodec> audioCodecPriorityList) {
        if (audioCodecPriorityList != null && !audioCodecPriorityList.isEmpty()) {
            this.audioCodecPriorityList = audioCodecPriorityList;
        }
        return this;
    }

    public AudioCodecSettings setAllowAudioMixedMimeTypess(boolean allowAudioMixedMimeTypes) {
        this.allowAudioMixedMimeTypes = allowAudioMixedMimeTypes;
        return this;
    }
}
