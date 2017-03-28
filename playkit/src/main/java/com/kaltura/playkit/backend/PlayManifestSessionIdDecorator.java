package com.kaltura.playkit.backend;

import android.net.Uri;

import com.kaltura.playkit.UrlDecorator;

import java.util.UUID;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class PlayManifestSessionIdDecorator implements UrlDecorator {
    
    private UUID playSessionId;

    public PlayManifestSessionIdDecorator() {
        playSessionId = UUID.randomUUID();
    }

    @Override
    public Uri getDecoratedUrl(Uri url) {

        if (url.getPath().contains("/playManifest/")) {
            return url.buildUpon()
                    .appendQueryParameter("playSessionId", playSessionId.toString())
                    .appendQueryParameter("clientTag", CLIENT_TAG)
                    .build();
        }

        return url;
    }
}
