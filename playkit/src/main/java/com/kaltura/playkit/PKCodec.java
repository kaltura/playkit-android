package com.kaltura.playkit;

public enum PKCodec {
    HEVC("hev1"),
   // hvc1("hvc1"),
    AVC("avc1");
   // avc3("avc3");

    public String codecName;

    PKCodec(String codecName){
        this.codecName = codecName;
    }
}
