package com.kaltura.playkit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;

public class LocalAssetsManagerHelper {
    private static final PKLog log = PKLog.get("LocalAssetsManagerHelper");

    private static final String ASSET_ID_PREFIX = "assetId:";
    final Context context;
    final LocalDataStore localDataStore;
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    PKRequestParams.Adapter licenseRequestParamAdapter;

    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        this.licenseRequestParamAdapter = licenseRequestAdapter;
    }

    LocalAssetsManagerHelper(Context context, LocalDataStore localDataStore) {
        this.context = context;
        this.localDataStore = localDataStore;

        MediaSupport.initializeDrm(context, null);
    }

    LocalAssetsManagerHelper(Context context) {
        this(context, new LocalAssetsManager.DefaultLocalDataStore(context));
    }

    static String buildAssetKey(String assetId) {
        return ASSET_ID_PREFIX + assetId;
    }

    /**
     * Check if network connection is available.
     *
     * @return - true if there is network connection, otherwise - false.
     */
    boolean isOnline() {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr == null) {
            return false;
        }
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable());
    }


    @Nullable
    PKDrmParams.Scheme getLocalAssetScheme(@NonNull String assetId) {
        try {
            String mediaFormatValue = new String(localDataStore.load(buildAssetKey(assetId)));
            String[] splitFormatValue = mediaFormatValue.split(":");
            if (splitFormatValue.length < 2) {
                return null;
            }
            String schemeName = splitFormatValue[1];
            if (schemeName.equals("null")) {
                return null;
            }

            return PKDrmParams.Scheme.valueOf(schemeName);

        } catch (FileNotFoundException e) {
            log.e(e.getMessage());
            return null;
        }
    }

    void saveMediaFormat(String assetId, PKMediaFormat mediaFormat, PKDrmParams.Scheme scheme) {
        localDataStore.save(buildAssetKey(assetId), buildMediaFormatValueAsByteArray(mediaFormat, scheme));
    }

    void removeAssetKey(String assetId) {
        localDataStore.remove(buildAssetKey(assetId));
    }

    private byte[] buildMediaFormatValueAsByteArray(PKMediaFormat mediaFormat, PKDrmParams.Scheme scheme) {
        String mediaFormatName = mediaFormat.toString();
        String schemeName = "null";
        if (scheme != null) {
            schemeName = scheme.toString();
        }
        String stringBuilder = mediaFormatName +
                ":" +
                schemeName;
        return stringBuilder.getBytes();
    }

}
