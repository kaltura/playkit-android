package com.kaltura.playkit.player.simid;

public interface SimidControllerProxy {
    void receiveMessage(String messageStr);
    void postMessage(String messageStr);
}
