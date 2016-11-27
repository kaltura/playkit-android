package com.kaltura.playkit.player;

import android.annotation.TargetApi;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.Assert;

import java.io.IOException;
import java.util.UUID;

/**
 * A {@link MediaDrmCallback} that makes requests using {@link HttpDataSource} instances.
 */
@TargetApi(18)
class DeferredMediaDrmCallback implements MediaDrmCallback {

    interface UrlProvider {
        String getUrl();
    }
    
    private final HttpDataSource.Factory dataSourceFactory;
    private final UrlProvider licenseUrlProvider;

    /**
     * @param dataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
     */
    DeferredMediaDrmCallback(HttpDataSource.Factory dataSourceFactory, UrlProvider licenseUrlProvider) {
        this.dataSourceFactory = dataSourceFactory;
        this.licenseUrlProvider = licenseUrlProvider;
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws IOException {

        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(null, dataSourceFactory);
        return httpMediaDrmCallback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        String licenseUrl = licenseUrlProvider.getUrl();
        Assert.checkNotEmpty(licenseUrl, "License URL not set!");

        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(licenseUrl, dataSourceFactory);
        
        return httpMediaDrmCallback.executeKeyRequest(uuid, request);
    }
}
