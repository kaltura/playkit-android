package com.kaltura.playkit;

public enum PKVideoCodec {
    HEVC("hev1"),
   // hvc1("hvc1"),
    AVC("avc1");
   // avc3("avc3");

    public String codecName;

    PKVideoCodec(String codecName){
        this.codecName = codecName;
    }
}
