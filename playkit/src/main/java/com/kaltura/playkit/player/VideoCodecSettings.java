package com.kaltura.playkit.player;

import com.kaltura.playkit.PKVideoCodec;

import java.util.ArrayList;
import java.util.List;

public class VideoCodecSettings {
    private List<PKVideoCodec> codecPriorityList = new ArrayList<>();
    private boolean allowSoftwareDecoder = false;
    private boolean allowMixedCodecAdaptiveness = false;

    public VideoCodecSettings() {
        codecPriorityList = getDefaultCodecsPriorityList();
    }

    public VideoCodecSettings(List<PKVideoCodec> codecPriorityList, boolean allowSoftwareDecoder, boolean allowMixedCodecAdaptiveness) {
        if (codecPriorityList != null && !codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        this.allowMixedCodecAdaptiveness = allowMixedCodecAdaptiveness;
    }

    public List<PKVideoCodec> getCodecPriorityList() {
        return codecPriorityList;
    }

    public boolean isAllowSoftwareDecoder() {
        return allowSoftwareDecoder;
    }

    public boolean getAllowMixedCodecAdaptiveness() {
        return allowMixedCodecAdaptiveness;
    }

    private List<PKVideoCodec> getDefaultCodecsPriorityList() {
        if (codecPriorityList == null) {
            codecPriorityList = new ArrayList<>();
        }
        codecPriorityList.add(PKVideoCodec.HEVC);

        codecPriorityList.add(PKVideoCodec.AV1);

        codecPriorityList.add(PKVideoCodec.VP9);

        codecPriorityList.add(PKVideoCodec.VP8);

        codecPriorityList.add(PKVideoCodec.AVC);

        return codecPriorityList;
    }
    
    public VideoCodecSettings setCodecPriorityList(List<PKVideoCodec> codecPriorityList) {
        if (codecPriorityList != null && !codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        }
        return this;
    }

    public VideoCodecSettings setAllowSoftwareDecoder(boolean allowSoftwareDecoder) {
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        return this;
    }

    public VideoCodecSettings setAllowMixedCodecAdaptiveness(boolean allowMixedCodecAdaptiveness) {
        this.allowMixedCodecAdaptiveness = allowMixedCodecAdaptiveness;
        return this;
    }
}
