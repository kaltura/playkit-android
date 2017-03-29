package com.kaltura.playkit.backend;

import android.net.Uri;

import com.kaltura.playkit.PKRequestInfo;
import com.kaltura.playkit.Player;

import java.util.UUID;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class PlayManifestSessionIdDecorator implements PKRequestInfo.Decorator {
    
    private UUID playSessionId;

    public PlayManifestSessionIdDecorator(UUID playSessionId) {
        this.playSessionId = playSessionId;
    }
    
    public PlayManifestSessionIdDecorator(Player player) {
        this.playSessionId = player.getSessionId();
    }

    @Override
    public PKRequestInfo getRequestInfo(PKRequestInfo requestInfo) {
        Uri url = requestInfo.getUrl();
        if (url.getPath().contains("/playManifest/")) {
            Uri alt = url.buildUpon()
                    .appendQueryParameter("clientTag", CLIENT_TAG)
                    .appendQueryParameter("playSessionId", playSessionId.toString())
                    .build();
            return new PKRequestInfo(alt, requestInfo.getHeaders());
        }

        return requestInfo;
    }
}
