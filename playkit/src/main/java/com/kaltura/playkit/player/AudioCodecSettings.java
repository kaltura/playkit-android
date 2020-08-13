package com.kaltura.playkit.player;

import com.kaltura.playkit.PKAudioCodec;

import java.util.ArrayList;
import java.util.List;

public class AudioCodecSettings {
    private List<PKAudioCodec> codecPriorityList = new ArrayList<>();
    private boolean allowAudioMixedCodecs = false;

    public AudioCodecSettings() {
        codecPriorityList = getDefaultCodecsPriorityList();
    }

    public AudioCodecSettings(List<PKAudioCodec> codecPriorityList, boolean allowAudioMixedCodecs) {
        if (codecPriorityList != null || codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowAudioMixedCodecs = allowAudioMixedCodecs;
    }

    public List<PKAudioCodec> getCodecPriorityList() {
        return codecPriorityList;
    }

    public boolean getAllowAudioMixedCodecs() {
        return allowAudioMixedCodecs;
    }

    private List<PKAudioCodec> getDefaultCodecsPriorityList() {
        if (codecPriorityList == null) {
            codecPriorityList = new ArrayList<>();
        }
        codecPriorityList.add(PKAudioCodec.E_AC3);

        codecPriorityList.add(PKAudioCodec.AC3);

        codecPriorityList.add(PKAudioCodec.OPUS);

        codecPriorityList.add(PKAudioCodec.AAC);

        return codecPriorityList;
    }

    public AudioCodecSettings setCodecPriorityList(List<PKAudioCodec> codecPriorityList) {
        if (codecPriorityList != null && !codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        }
        return this;
    }

    public AudioCodecSettings setAllowAudioMixedMimeTypess(boolean allowAudioMixedMimeTypes) {
        this.allowAudioMixedCodecs = allowAudioMixedMimeTypes;
        return this;
    }
}
