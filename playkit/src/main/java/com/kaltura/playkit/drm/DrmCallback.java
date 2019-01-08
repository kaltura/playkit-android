package com.kaltura.playkit.drm;

import android.net.Uri;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.PKRequestParams;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DrmCallback implements MediaDrmCallback {
    private final HttpDataSource.Factory dataSourceFactory;
    private final PKRequestParams.Adapter adapter;
    private HttpMediaDrmCallback callback;

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws IOException {
        return callback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        return callback.executeKeyRequest(uuid, request);
    }

    public DrmCallback(HttpDataSource.Factory dataSourceFactory, PKRequestParams.Adapter adapter) {
        this.dataSourceFactory = dataSourceFactory;
        this.adapter = adapter;
    }

    public void setLicenseUrl(String licenseUrl) {
        PKRequestParams params = new PKRequestParams(Uri.parse(licenseUrl), new HashMap<>());

        if (adapter != null) {
            params = adapter.adapt(params);
        }

        callback = new HttpMediaDrmCallback(params.url.toString(), dataSourceFactory);

        for (Map.Entry<String, String> entry : params.headers.entrySet()) {
            callback.setKeyRequestProperty(entry.getKey(), entry.getValue());
        }
    }
}
