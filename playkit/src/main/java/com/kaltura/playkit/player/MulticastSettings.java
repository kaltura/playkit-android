package com.kaltura.playkit.player;

public class MulticastSettings {

    private boolean useExoDefaultSettings = true;
    // maxPacketSize The maximum datagram packet size, in bytes.
    private int maxPacketSize = 3000;
    // socketTimeoutMillis The socket timeout in milliseconds. A timeout of zero is interpreted
    private int socketTimeoutMillis = 10000;

    private ExtractorMode extractorMode = ExtractorMode.MODE_MULTI_PMT;

    enum ExtractorMode {
        MODE_MULTI_PMT(0),
        MODE_SINGLE_PMT(1),
        MODE_HLS(2);

        public final int mode;
        ExtractorMode(int mode) {
            this.mode = mode;
        }
    }

    public MulticastSettings() {}

    public boolean getUseExoDefaultSettings() {
        return useExoDefaultSettings;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public MulticastSettings setUseExoDefaultSettings(boolean useExoDefaultSettings) {
        this.useExoDefaultSettings = useExoDefaultSettings;
        return this;
    }

    public MulticastSettings setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
        this.useExoDefaultSettings = false;
        return this;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public MulticastSettings setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
        this.useExoDefaultSettings = false;
        return this;
    }

    public ExtractorMode getExtractorMode() {
        return extractorMode;
    }

    public MulticastSettings setExtractorMode(ExtractorMode extractorMode) {
        if (extractorMode != null) {
            this.extractorMode = extractorMode;
        }
        this.useExoDefaultSettings = false;
        return this;
    }
}
