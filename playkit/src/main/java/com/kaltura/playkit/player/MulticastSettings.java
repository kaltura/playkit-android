package com.kaltura.playkit.player;

public class MulticastSettings {

    // maxPacketSize The maximum datagram packet size, in bytes.
    private int maxPacketSize = 3000;
    // socketTimeoutMillis The socket timeout in milliseconds. A timeout of zero is interpreted
    private int socketTimeoutMillis = 10000;

    public MulticastSettings() {}

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public MulticastSettings setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
        return this;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public MulticastSettings setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
        return this;
    }
}
