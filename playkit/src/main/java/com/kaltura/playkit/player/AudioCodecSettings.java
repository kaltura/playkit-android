package com.kaltura.playkit.player;

import com.kaltura.playkit.PKAudioCodec;

import java.util.ArrayList;
import java.util.List;

public class AudioCodecSettings {
    private List<PKAudioCodec> audioCodecPriorityList = new ArrayList<>();
    private boolean allowSoftwareDecoder = false;
    private boolean allowAudioMixedMimeTypeAdaptiveness = false;

    public AudioCodecSettings() {
        audioCodecPriorityList = getDefaultCodecsPriorityList();
    }

    public AudioCodecSettings(List<PKAudioCodec> audioCodecPriorityList, boolean allowSoftwareDecoder, boolean allowAudioMixedMimeTypeAdaptiveness) {
        if (audioCodecPriorityList != null || audioCodecPriorityList.isEmpty()) {
            this.audioCodecPriorityList = audioCodecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        this.allowAudioMixedMimeTypeAdaptiveness = allowAudioMixedMimeTypeAdaptiveness;
    }

    public List<PKAudioCodec> getAudioCodecPriorityList() {
        return audioCodecPriorityList;
    }

    public boolean isAllowSoftwareDecoder() {
        return allowSoftwareDecoder;
    }

    public boolean getAllowAudioMixedMimeTypeAdaptiveness() {
        return allowAudioMixedMimeTypeAdaptiveness;
    }

    private List<PKAudioCodec> getDefaultCodecsPriorityList() {
        if (audioCodecPriorityList == null) {
            audioCodecPriorityList = new ArrayList<>();
        }
        audioCodecPriorityList.add(PKAudioCodec.AC3);

        audioCodecPriorityList.add(PKAudioCodec.E_AC3);

        audioCodecPriorityList.add(PKAudioCodec.AAC);

        return audioCodecPriorityList;
    }

    public AudioCodecSettings setAudioCodecPriorityList(List<PKAudioCodec> audioCodecPriorityList) {
        if (audioCodecPriorityList != null && !audioCodecPriorityList.isEmpty()) {
            this.audioCodecPriorityList = audioCodecPriorityList;
        }
        return this;
    }

    public AudioCodecSettings setAllowSoftwareDecoder(boolean allowSoftwareDecoder) {
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        return this;
    }

    public AudioCodecSettings setAllowAudioMixedMimeTypeAdaptiveness(boolean allowAudioMixedMimeTypeAdaptiveness) {
        this.allowAudioMixedMimeTypeAdaptiveness = allowAudioMixedMimeTypeAdaptiveness;
        return this;
    }
}
