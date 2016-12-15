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
                    String licenseUri = mediaSource.getDrmData().get(0).getLicenseUri(); //TODO filter and select the correct drmData based on DrmAdapter.DRMScheme
                    drmAdapter.registerAsset(localAssetPath, licenseUri, listener);

                } catch (IOException e) {
                    log.e("Error", e);
                    if (listener != null) {
                        listener.onFailed(localAssetPath, e);
                    }
                }

            }
        });

        return true;
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
