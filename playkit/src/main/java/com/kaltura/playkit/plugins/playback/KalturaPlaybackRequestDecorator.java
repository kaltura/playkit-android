package com.kaltura.playkit.plugins.playback;

import android.net.Uri;

import com.kaltura.playkit.PKRequestInfo;
import com.kaltura.playkit.Player;

import java.util.UUID;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class KalturaPlaybackRequestDecorator implements PKRequestInfo.Decorator {
    
    private UUID playSessionId;
    
    public static void setup(Player player) {
        KalturaPlaybackRequestDecorator decorator = new KalturaPlaybackRequestDecorator(player.getSessionId());
        player.getSettings().setContentRequestDecorator(decorator);
    }

    private KalturaPlaybackRequestDecorator(UUID playSessionId) {
        this.playSessionId = playSessionId;
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
