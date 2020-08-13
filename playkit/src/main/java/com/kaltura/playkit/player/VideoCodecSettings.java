package com.kaltura.playkit.player;

import com.kaltura.playkit.PKVideoCodec;

import java.util.ArrayList;
import java.util.List;

public class VideoCodecSettings {
    private List<PKVideoCodec> codecPriorityList = new ArrayList<>();
    private boolean allowSoftwareDecoder = false;
    private boolean allowVideoMixedCodecAdaptiveness = false;

    public VideoCodecSettings() {
        codecPriorityList = getDefaultCodecsPriorityList();
    }

    public VideoCodecSettings(List<PKVideoCodec> codecPriorityList, boolean allowSoftwareDecoder, boolean allowVideoMixedCodecAdaptiveness) {
        if (codecPriorityList != null || codecPriorityList.isEmpty()) {
            this.codecPriorityList = codecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        this.allowVideoMixedCodecAdaptiveness = allowVideoMixedCodecAdaptiveness;
    }

    public List<PKVideoCodec> getCodecPriorityList() {
        return codecPriorityList;
    }

    public boolean isAllowSoftwareDecoder() {
        return allowSoftwareDecoder;
    }

    public boolean getAllowVideoMixedCodecAdaptiveness() {
        return allowVideoMixedCodecAdaptiveness;
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

    public VideoCodecSettings setAllowVideoMixedCodecAdaptiveness(boolean allowVideoMixedCodecAdaptiveness) {
        this.allowVideoMixedCodecAdaptiveness = allowVideoMixedCodecAdaptiveness;
        return this;
    }
}
