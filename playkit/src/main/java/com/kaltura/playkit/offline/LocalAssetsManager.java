package com.kaltura.playkit.offline;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public class LocalAssetsManager {

    private static final PKLog log = PKLog.get("LocalAssetsManager");

    private static final int JSON_BYTE_LIMIT = 1024 * 1024;
    private static OfflineStorage offlineStorage;


    public interface AssetRegistrationListener {
        void onRegistered(String assetPath);
        void onFailed(String assetPath, Exception error);
    }

    public interface AssetStatusListener {
        void onStatus(String assetPath, long expiryTimeSeconds, long availableTimeSeconds);
    }

    public interface AssetRemovalListener {
        void onRemoved(String assetPath);
    }


    public LocalAssetsManager(OfflineStorage offlineStorage){
        this.offlineStorage = offlineStorage;
    }

    public static boolean registerAsset(@NonNull final Context context, @NonNull final PKMediaSource mediaSource,
                                        @NonNull final String localAssetPath, @Nullable final AssetRegistrationListener listener) {

        return registerOrRefreshAsset(context, mediaSource, localAssetPath, false, listener);
    }

    public static boolean refreshAsset(@NonNull final Context context, @NonNull final PKMediaSource mediaSource,
                                       @NonNull final String loaclAssetPath, @Nullable final AssetRegistrationListener listener) {


        return registerOrRefreshAsset(context, mediaSource, loaclAssetPath, true, listener);

    }

    public static boolean unregisterAsset(@NonNull final Context context, @NonNull final PKMediaSource mediaSource,
                                          @NonNull final String localAssetPath, final AssetRemovalListener listener) {

        doInBackground(new Runnable() {
            @Override
            public void run() {
                // Remove cache
                    DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, offlineStorage, localAssetPath);
                    drmAdapter.unregisterAsset(localAssetPath, listener);
            }
        });
        return true;
    }

    public static boolean checkAssetStatus(@NonNull final Context context, @NonNull final String localAssetPath,
                                           @Nullable final AssetStatusListener listener) {

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, offlineStorage, localAssetPath);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.checkAssetStatus(localAssetPath, listener);
            }
        });

        return true;
    }

    private static boolean registerOrRefreshAsset(@NonNull final Context context, @NonNull final PKMediaSource mediaSource,
                                                  @NonNull final String localAssetPath, final boolean refresh, @Nullable final AssetRegistrationListener listener) {

        // Preflight: check that all parameters are valid.
        checkNotNull(mediaSource.getUrl(), "mediaSource.url");    // can be an empty string (but not null)
        checkNotEmpty(localAssetPath, "localAssetPath");


        if (!isOnline(context)) {
            log.i("Can't register/refresh when offline");
            return false;
        }

        doInBackground(new Runnable() {
            @Override
            public void run() {

                try {

                    DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, offlineStorage, localAssetPath);
                    DrmAdapter.DRMScheme scheme = drmAdapter.getScheme();

                    Uri licenseUri = prepareLicenseUri(entry, flavor, scheme);
                    drmAdapter.registerAsset(localAssetPath, String.valueOf(licenseUri), listener);

                } catch (JSONException | IOException e) {
                    log.e("Error", e);
                    if (listener != null) {
                        listener.onFailed(localAssetPath, e);
                    }
                }

            }
        });

        return true;
    }

    private static Uri prepareLicenseUri(KPPlayerConfig config, @Nullable String flavor, @NonNull DrmAdapter.DRMScheme drmScheme) throws IOException, JSONException {

        String overrideUrl = config.getConfigValueString("Kaltura.overrideDrmServerURL");
        if (overrideUrl != null) {
            return Uri.parse(overrideUrl);
        }

        if (drmScheme == DrmAdapter.DRMScheme.Null) {
            return null;
        }

        // load license data
        Uri getLicenseDataURL = prepareGetLicenseDataURL(config, flavor, drmScheme);
        String licenseData = Utilities.loadStringFromURL(getLicenseDataURL, JSON_BYTE_LIMIT);

        // parse license data
        JSONObject licenseDataJSON = new JSONObject(licenseData);
        if (licenseDataJSON.has("error")) {
            throw new IOException("Error getting license data: " + licenseDataJSON.getJSONObject("error").getString("message"));
        }

        String licenseUri = licenseDataJSON.getString("licenseUri");

        return Uri.parse(licenseUri);
    }

    private static Uri prepareGetLicenseDataURL(KPPlayerConfig config, String flavor, DrmAdapter.DRMScheme drmScheme) throws IOException, JSONException {

        Uri serviceURL = Uri.parse(config.getServerURL());
        // URL may either point to the root of the server or to mwEmbedFrame.php. Resolve this.
        if (serviceURL.getPath().endsWith("/mwEmbedFrame.php")) {
            serviceURL = Utilities.stripLastUriPathSegment(serviceURL);
        } else {
            serviceURL = resolvePlayerRootURL(serviceURL, config.getPartnerId(), config.getUiConfId(), config.getKS());
        }

        // Now serviceURL is something like "http://cdnapi.kaltura.com/html5/html5lib/v2.38.3".


        String drmName = null;
        switch (drmScheme) {
            case WidevineCENC:
                drmName = "wvcenc";
                break;
            case WidevineClassic:
                drmName = "wvclassic";
                break;
        }

        return serviceURL.buildUpon()
                .appendPath("services.php")
                .encodedQuery(config.getQueryString())
                .appendQueryParameter("service", "getLicenseData")
                .appendQueryParameter("drm", drmName)
                .appendQueryParameter("flavor_id", flavor).build();
    }

    private static Uri resolvePlayerRootURL(Uri serverURL, String partnerId, String uiConfId, String ks) throws IOException, JSONException {
        // serverURL is something like "http://cdnapi.kaltura.com";
        // we need to get to "http://cdnapi.kaltura.com/html5/html5lib/v2.38.3".
        // This is done by loading UIConf data, and looking at "html5Url" property.

        String jsonString = loadUIConf(serverURL, partnerId, uiConfId, ks);
        String embedLoaderUrl;
        JSONObject uiConfJSON = new JSONObject(jsonString);
        if (uiConfJSON.has("message")) {
            throw new IOException("Error getting UIConf: " + uiConfJSON.getString("message"));
        }
        embedLoaderUrl = uiConfJSON.getString("html5Url");

        Uri serviceUri;
        if (embedLoaderUrl.startsWith("/")) {
            serviceUri = serverURL.buildUpon()
                    .appendEncodedPath(embedLoaderUrl)
                    .build();
        } else {
            serviceUri = Uri.parse(embedLoaderUrl);
        }

        return Utilities.stripLastUriPathSegment(serviceUri);
    }

    private static String loadUIConf(Uri serverURL, String partnerId, String uiConfId, String ks) throws IOException {

        Uri.Builder uriBuilder = serverURL.buildUpon()
                .appendEncodedPath("api_v3/index.php")
                .appendQueryParameter("service", "uiconf")
                .appendQueryParameter("action", "get")
                .appendQueryParameter("format", "1")
                .appendQueryParameter("p", partnerId)
                .appendQueryParameter("id", uiConfId);

        if (ks != null) {
            uriBuilder.appendQueryParameter("ks", ks);
        }

        return Utilities.loadStringFromURL(uriBuilder.build(), JSON_BYTE_LIMIT);
    }

    private static void checkArg(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void checkNotNull(Object obj, String name) {
        checkArg(obj == null, name + " must not be null");
    }

    private static void checkNotEmpty(String obj, String name) {
        checkArg(obj == null || obj.length() == 0, name + " must not be empty");
    }

    private static void doInBackground(Runnable runnable) {
        new Thread(runnable).start();
    }

    private static boolean isOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable());
    }

}
