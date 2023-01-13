package com.kaltura.playkit.drm;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.drm.ExoMediaDrm;
import com.kaltura.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.kaltura.android.exoplayer2.drm.MediaDrmCallback;
import com.kaltura.android.exoplayer2.drm.MediaDrmCallbackException;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DataSourceInputStream;
import com.kaltura.android.exoplayer2.upstream.DataSpec;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.upstream.StatsDataSource;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.player.MediaSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DrmCallback implements MediaDrmCallback {
    private static final PKLog log = PKLog.get("DrmCallback");

    private final HttpDataSource.Factory dataSourceFactory;
    private final PKRequestParams.Adapter adapter;
    private HttpMediaDrmCallback callback;
    private final Map<String, String> postBodyMap = new HashMap<>();
    private String licenseUrl;
    private final String KEY_DRM_INFO = "drm_info";

    public DrmCallback(HttpDataSource.Factory dataSourceFactory, PKRequestParams.Adapter adapter) {
        this.dataSourceFactory = dataSourceFactory;
        this.adapter = adapter;
    }

    @NonNull
    @Override
    public byte[] executeProvisionRequest(@NonNull UUID uuid, @NonNull ExoMediaDrm.ProvisionRequest request) throws MediaDrmCallbackException {
        return callback.executeProvisionRequest(uuid, request);
    }

    @NonNull
    @Override
    public byte[] executeKeyRequest(@NonNull UUID uuid, @NonNull ExoMediaDrm.KeyRequest request) throws MediaDrmCallbackException {
        if (adapter != null && !postBodyMap.isEmpty()) {
            if (TextUtils.isEmpty(licenseUrl)) {
                throw new MediaDrmCallbackException(
                        new DataSpec.Builder().setUri(Uri.EMPTY).build(),
                        Uri.EMPTY,
                        ImmutableMap.of(),
                        0,
                        new IllegalStateException("No license URL"));
            }
            Map<String, String> requestProperties = new HashMap<>();
            // Add standard request properties for supported schemes.
            String contentType =
                    MediaSupport.PLAYREADY_UUID.equals(uuid)
                            ? "text/xml"
                            : (C.CLEARKEY_UUID.equals(uuid) ? "application/json" : "application/octet-stream");
            requestProperties.put("Content-Type", contentType);
            if (MediaSupport.PLAYREADY_UUID.equals(uuid)) {
                requestProperties.put(
                        "SOAPAction", "http://schemas.microsoft.com/DRM/2007/03/protocols/AcquireLicense");
            }
            JSONObject postBodyJsonObject;
            try {
                postBodyJsonObject = getPostBodyJson(request.getData(), postBodyMap);
                return executePost(dataSourceFactory, licenseUrl, postBodyJsonObject.toString().getBytes() , requestProperties);
            } catch (JSONException e) {
                throw new MediaDrmCallbackException(
                        new DataSpec.Builder().setUri(licenseUrl).build(),
                        Uri.EMPTY,
                        ImmutableMap.of(),
                        0,
                        new JSONException(e.getMessage()));
            }
        } else {
            return callback.executeKeyRequest(uuid, request);
        }
    }

    void setLicenseUrl(String licenseUrl) {

        if (licenseUrl == null) {
            log.e("Invalid license URL = null");
            return;
        }

        PKRequestParams params = new PKRequestParams(Uri.parse(licenseUrl), new HashMap<>());

        if (adapter != null) {
            params = adapter.adapt(params);
            if (params.postBody != null && !params.postBody.isEmpty()) {
                postBodyMap.putAll(params.postBody);
            }
            if (params.url != null) {
                this.licenseUrl = licenseUrl;
            } else {
                log.e("Adapter returned null license URL");
                return;
            }
        }

        callback = new HttpMediaDrmCallback(params.url.toString(), dataSourceFactory);

        for (Map.Entry<String, String> entry : params.headers.entrySet()) {
            callback.setKeyRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Create the post JSON payload having custom post key/values and drm data
     * @param data drm data
     * @param requestProperties custom post params
     * @return JSONObject
     * @throws JSONException if there is some exception in JSON creation
     */
    @NonNull
    private JSONObject getPostBodyJson(byte[] data, Map<String, String> requestProperties) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry: requestProperties.entrySet()) {
            if (TextUtils.equals(entry.getKey(), KEY_DRM_INFO)) {
                JSONArray jsonArray = doMaskingOnDrmInfo(data);
                if (jsonArray == null) {
                    continue;
                }
                json.put(KEY_DRM_INFO, jsonArray);
            } else {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        return json;
    }

    /**
     * Do bitmask for the drm data
     * @param data drm data
     * @return JSONArray of the bitmasked drm data
     */
    @Nullable
    private JSONArray doMaskingOnDrmInfo(@Nullable byte[] data) {
        if (data == null) {
            log.e("Invalid key request data");
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        int bitmask = 0x000000FF;
        for (byte aData : data) {
            jsonArray.put(bitmask & aData);
        }
        return jsonArray;
    }

    /**
     * Do network call with customized post params and get the key byte data in response
     * @throws MediaDrmCallbackException
     */
    private static byte[] executePost(
            DataSource.Factory dataSourceFactory,
            String url,
            @Nullable byte[] httpBody,
            Map<String, String> requestProperties)
            throws MediaDrmCallbackException {
        StatsDataSource dataSource = new StatsDataSource(dataSourceFactory.createDataSource());
        int manualRedirectCount = 0;
        DataSpec dataSpec =
                new DataSpec.Builder()
                        .setUri(url)
                        .setHttpRequestHeaders(requestProperties)
                        .setHttpMethod(DataSpec.HTTP_METHOD_POST)
                        .setHttpBody(httpBody)
                        .setFlags(DataSpec.FLAG_ALLOW_GZIP)
                        .build();
        DataSpec originalDataSpec = dataSpec;
        try {
            while (true) {
                DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
                try {
                    return Util.toByteArray(inputStream);
                } catch (HttpDataSource.InvalidResponseCodeException e) {
                    @Nullable String redirectUrl = getRedirectUrl(e, manualRedirectCount);
                    if (redirectUrl == null) {
                        throw e;
                    }
                    manualRedirectCount++;
                    dataSpec = dataSpec.buildUpon().setUri(redirectUrl).build();
                } finally {
                    Util.closeQuietly(inputStream);
                }
            }
        } catch (Exception e) {
            throw new MediaDrmCallbackException(
                    originalDataSpec,
                    dataSource.getLastOpenedUri(),
                    dataSource.getResponseHeaders(),
                    dataSource.getBytesRead(),
                    /* cause= */ e);
        }
    }

    @Nullable
    private static String getRedirectUrl(
            HttpDataSource.InvalidResponseCodeException exception, int manualRedirectCount) {
        // For POST requests, the underlying network stack will not normally follow 307 or 308
        // redirects automatically. Do so manually here.
        boolean manuallyRedirect =
                (exception.responseCode == 307 || exception.responseCode == 308)
                        && manualRedirectCount < 10;
        if (!manuallyRedirect) {
            return null;
        }
        Map<String, List<String>> headerFields = exception.headerFields;
        if (headerFields != null) {
            @Nullable List<String> locationHeaders = headerFields.get("Location");
            if (locationHeaders != null && !locationHeaders.isEmpty()) {
                return locationHeaders.get(0);
            }
        }
        return null;
    }
}
