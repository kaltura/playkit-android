package com.kaltura.playkit.plugins.playback;

import android.content.Context;
import android.net.Uri;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.Utils;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class KalturaPlaybackRequestAdapter implements PKRequestParams.Adapter {

    private final String packageName;
    private String playSessionId;
    
    public static void setup(Context context, Player player) {
        KalturaPlaybackRequestAdapter decorator = new KalturaPlaybackRequestAdapter(context.getPackageName(), player.getSessionId());
        player.getSettings().setContentRequestAdapter(decorator);
    }

    private KalturaPlaybackRequestAdapter(String packageName, String playSessionId) {
        this.packageName = packageName;
        this.playSessionId = playSessionId;
    }
    
    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {
        Uri url = requestParams.url;

        if (url.getPath().contains("/playManifest/")) {
            Uri alt = url.buildUpon()
                    .appendQueryParameter("clientTag", CLIENT_TAG)
                    .appendQueryParameter("referrer", Utils.encodeToString(packageName.getBytes()))
                    .appendQueryParameter("playSessionId", playSessionId)
                    .build();
            return new PKRequestParams(alt, requestParams.headers);
        }

        return requestParams;
    }

    @Override
    public void updateParams(Player player) {
        this.playSessionId = player.getSessionId();
    }
}
