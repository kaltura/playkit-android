package com.kaltura.playkit.player.metadata;

import androidx.annotation.NonNull;

public class URIConnectionAcquiredInfo {
    public String url;
    public long dnsDurationMs;
    public long tlsDurationMs;
    public long connectDurationMs;

    @NonNull
    @Override
    public String toString() {
        return "URL = " + url + "\ndnsDurationMs = " + dnsDurationMs + "\ntlsDurationMs = " + tlsDurationMs + "\nconnectDurationMs = " + connectDurationMs;
    }
}
