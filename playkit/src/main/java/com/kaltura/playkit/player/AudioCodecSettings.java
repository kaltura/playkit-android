package com.kaltura.playkit.player;

import com.kaltura.playkit.PKAudioCodec;

import java.util.ArrayList;
import java.util.List;

public class AudioCodecSettings {
    private List<PKAudioCodec> codecPriorityList = new ArrayList<>();
    private boolean allowMixedCodecs = false;
    private boolean allowMixedBitrates = false;

    public AudioCodecSettings() {
        codecPriorityList = getDefaultCodecsPriorityList();
    }

    public AudioCodecSettings(List<PKAudioCodec> codecPriorityList, boolean allowMixedCodecs) {
        if (codecPriorityList != null && !codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowMixedCodecs = allowMixedCodecs;
    }

    public List<PKAudioCodec> getCodecPriorityList() {
        return codecPriorityList;
    }

    public boolean getAllowMixedCodecs() {
        return allowMixedCodecs;
    }

    public boolean getAllowMixedBitrates() {
        return allowMixedBitrates;
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

    public AudioCodecSettings setAllowMixedCodecs(boolean allowMixedCodecs) {
        this.allowMixedCodecs = allowMixedCodecs;
        return this;
    }

    public AudioCodecSettings setAllowMixedBitrates(boolean allowMixedBitrates) {
        this.allowMixedBitrates = allowMixedBitrates;
        return this;
    }
}
