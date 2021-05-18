package com.kaltura.playkit;

public enum PKWakeMode {
    NONE,    // A wake mode that will not cause the player to hold any locks.
    LOCAL,   // A wake mode that will cause the player to hold a PowerManager.WakeLock during playback.
    NETWORK  // A wake mode that will cause the player to hold a PowerManager.WakeLock and a WifiManager.WifiLock during playback
}
