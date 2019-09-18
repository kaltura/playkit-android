package com.kaltura.playkit.player;

import com.kaltura.playkit.PKVideoCodec;

public class VideoCodecSettings {
    private PKVideoCodec videoCodec;
    private boolean softwareDecoderEnabled;

    public VideoCodecSettings() {
        this.videoCodec = PKVideoCodec.HEVC;
        this.softwareDecoderEnabled = false;
    }

    public VideoCodecSettings(PKVideoCodec videoCodec, boolean softwareDecoderEnabled) {
        this.videoCodec = videoCodec;
        this.softwareDecoderEnabled = softwareDecoderEnabled;
    }

    public PKVideoCodec getVideoCodec() {
        return videoCodec;
    }

    public VideoCodecSettings setVideoCodec(PKVideoCodec videoCodec) {
        this.videoCodec = videoCodec;
        return this;
    }

    public boolean isSoftwareDecoderEnabled() {
        return softwareDecoderEnabled;
    }

    public VideoCodecSettings setSoftwareDecoderEnabled(boolean softwareDecoderEnabled) {
        this.softwareDecoderEnabled = softwareDecoderEnabled;
        return this;
    }
}
