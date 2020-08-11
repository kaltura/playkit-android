package com.kaltura.playkit.player;

import com.kaltura.playkit.PKVideoCodec;

import java.util.ArrayList;
import java.util.List;

public class VideoCodecSettings {
    private List<PKVideoCodec> videoCodecPriorityList = new ArrayList<>();
    private boolean allowSoftwareDecoder = false;
    private boolean allowVideoMixedMimeTypeAdaptiveness = false;

    public VideoCodecSettings() {
        videoCodecPriorityList = getDefaultCodecsPriorityList();
    }

    public VideoCodecSettings(List<PKVideoCodec> videoCodecPriorityList, boolean allowSoftwareDecoder, boolean allowVideoMixedMimeTypeAdaptiveness) {
        if (videoCodecPriorityList != null || videoCodecPriorityList.isEmpty()) {
            this.videoCodecPriorityList = videoCodecPriorityList;
        } else {
            getDefaultCodecsPriorityList();
        }
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        this.allowVideoMixedMimeTypeAdaptiveness = allowVideoMixedMimeTypeAdaptiveness;
    }

    public List<PKVideoCodec> getVideoCodecPriorityList() {
        return videoCodecPriorityList;
    }

    public boolean isAllowSoftwareDecoder() {
        return allowSoftwareDecoder;
    }

    public boolean getAllowVideoMixedMimeTypeAdaptiveness() {
        return allowVideoMixedMimeTypeAdaptiveness;
    }

    private List<PKVideoCodec> getDefaultCodecsPriorityList() {
        if (videoCodecPriorityList == null) {
            videoCodecPriorityList = new ArrayList<>();
        }
        videoCodecPriorityList.add(PKVideoCodec.HEVC);

        videoCodecPriorityList.add(PKVideoCodec.AV1);

        videoCodecPriorityList.add(PKVideoCodec.VP9);

        videoCodecPriorityList.add(PKVideoCodec.VP8);

        videoCodecPriorityList.add(PKVideoCodec.AVC);

        return videoCodecPriorityList;
    }
    
    public VideoCodecSettings setVideoCodecPriorityList(List<PKVideoCodec> videoCodecPriorityList) {
        if (videoCodecPriorityList != null && !videoCodecPriorityList.isEmpty()) {
            this.videoCodecPriorityList = videoCodecPriorityList;
        }
        return this;
    }

    public VideoCodecSettings setAllowSoftwareDecoder(boolean allowSoftwareDecoder) {
        this.allowSoftwareDecoder = allowSoftwareDecoder;
        return this;
    }

    public VideoCodecSettings setAllowVideoMixedMimeTypeAdaptiveness(boolean allowVideoMixedMimeTypeAdaptiveness) {
        this.allowVideoMixedMimeTypeAdaptiveness = allowVideoMixedMimeTypeAdaptiveness;
        return this;
    }
}
