package com.kaltura.playkit.plugins.playback;

import android.net.Uri;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;

import java.util.UUID;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class KalturaPlaybackRequestAdapter implements PKRequestParams.Adapter {
    
    private UUID playSessionId;
    
    public static void setup(Player player) {
        KalturaPlaybackRequestAdapter decorator = new KalturaPlaybackRequestAdapter(player.getSessionId());
        player.getSettings().setContentRequestAdapter(decorator);
    }

    private KalturaPlaybackRequestAdapter(UUID playSessionId) {
        this.playSessionId = playSessionId;
    }
    
    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {
        Uri url = requestParams.url;
        if (url.getPath().contains("/playManifest/")) {
            Uri alt = url.buildUpon()
                    .appendQueryParameter("clientTag", CLIENT_TAG)
                    .appendQueryParameter("playSessionId", playSessionId.toString())
                    .build();
            return new PKRequestParams(alt, requestParams.headers);
        }

        return requestParams;
    }
}
