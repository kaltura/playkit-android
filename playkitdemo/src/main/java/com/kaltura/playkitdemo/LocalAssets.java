package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalDataStore;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;

import java.io.File;
import java.util.Collections;

/**
 * Created by Noam Tamim @ Kaltura on 14/02/2017.
 */

public class LocalAssets {
    private static final PKLog log = PKLog.get("LocalAssets");
    
    private PKDrmParams drmParams = new PKDrmParams("https://udrm.kaltura.com/widevine/license?custom_data=eyJjYV9zeXN0ZW0iOiJPVlAiLCJ1c2VyX3Rva2VuIjoiZGpKOE1UZzFNVFUzTVh4NThBVkFQOXo1R0lvU3BXWE95emEtdWlQNnk5cXpBdkpkMzZfUFZOcUNfT0NZWWhLRVh5LThqcGFMRktGcU15d3VTN2ZLZmxibTd1VVJGQkVtSGxsNkc4NEU2LUxrcnFXbVV1ZWEtZnFqdXc9PSIsImFjY291bnRfaWQiOiIxODUxNTcxIiwiY29udGVudF9pZCI6IjBfcGw1bGJmbzAiLCJmaWxlcyI6IjBfendxM2w0NHIsMF91YTYycms2cywwX290bWFxcG5mLDBfeXdrbXFua2csMV9lMHF0YWoxaiwxX2IycXp5dmE3In0%3D&signature=LFiNPZL8%2BNevsZ8cNhrmSDM4SDQ%3D", PKDrmParams.Scheme.WidevineClassic);
    private PKMediaSource widevineClassicSource = new PKMediaSource()
            .setId("wvc")
            .setDrmData(Collections.singletonList(drmParams))
            .setMediaFormat(PKMediaFormat.wvm)
            .setUrl("http://cdnapi.kaltura.com/p/1851571/playManifest/entryId/0_pl5lbfo0/format/url/tags/widevine/protocol/http/a.wvm");
    private String localAssetPath;

    private PKMediaEntry widevineClassicEntry = new PKMediaEntry().setId("e1").setMediaType(PKMediaEntry.MediaEntryType.Vod)
            .setSources(Collections.singletonList(widevineClassicSource));
    private LocalAssetsManager localAssetsManager;

    public LocalAssets(Context context) {

        LocalDataStore localDataStore = new LocalAssetsManager.DefaultLocalDataStore(context);
        localAssetsManager = new LocalAssetsManager(context, localDataStore);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, "0_pl5lbfo0.wvm");
        localAssetPath = file.getAbsolutePath();
    }

    static void start(final Context context, final OnMediaLoadCompletion completion) {

        final LocalAssets localAssets = new LocalAssets(context);

        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle("Local Assets")
                .setNegativeButton("Hide", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                alertBuilder.show();
                            }
                        }, 5000);
                    }
                })
                .setItems(new String[]{"Register", "Check Status", "Play", "Unregister"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        log.d("clicked: " + which);
                        switch (which) {
                            case 0:
                                localAssets.registerAsset(new LocalAssetsManager.AssetRegistrationListener() {
                                    @Override
                                    public void onRegistered(String localAssetPath) {
                                        Toast.makeText(context, "Registered", Toast.LENGTH_LONG).show();
                                        alertBuilder.show();
                                    }

                                    @Override
                                    public void onFailed(String localAssetPath, Exception error) {
                                        Toast.makeText(context, "Failed: " + error, Toast.LENGTH_LONG).show();
                                        alertBuilder.show();
                                    }
                                });
                                break;
                            case 1:
                                localAssets.checkAssetStatus(new LocalAssetsManager.AssetStatusListener() {
                                    @Override
                                    public void onStatus(String localAssetPath, long expiryTimeSeconds, long availableTimeSeconds, boolean isRegistered) {
                                        Toast.makeText(context, "Status: " + isRegistered + ", " + expiryTimeSeconds + ", " + availableTimeSeconds, Toast.LENGTH_LONG).show();
                                        alertBuilder.show();
                                    }
                                });
                                break;
                            case 2:
                                localAssets.getLocalMediaEntry(completion);
                                alertBuilder.show();
                                break;
                            case 3:
                                localAssets.unregisterAsset(new LocalAssetsManager.AssetRemovalListener() {
                                    @Override
                                    public void onRemoved(String localAssetPath) {
                                        Toast.makeText(context, "Unregistered:", Toast.LENGTH_LONG).show();
                                    }
                                });
                                alertBuilder.show();
                                break;
                        }
                    }
                });

        alertBuilder.show();
    }

    public void registerAsset(LocalAssetsManager.AssetRegistrationListener listener) {
        localAssetsManager.registerAsset(widevineClassicSource, localAssetPath, widevineClassicSource.getId(), listener);
    }
    
    public void getLocalMediaEntry(OnMediaLoadCompletion completion) {
        final PKMediaSource localMediaSource = localAssetsManager.getLocalMediaSource(widevineClassicSource.getId(), localAssetPath);
        completion.onComplete(new ResultElement<PKMediaEntry>() {

            @Override
            public PKMediaEntry getResponse() {
                return new PKMediaEntry()
                        .setMediaType(PKMediaEntry.MediaEntryType.Vod)
                        .setId(widevineClassicEntry.getId())
                        .setSources(Collections.singletonList(localMediaSource));
            }

            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public ErrorElement getError() {
                return null;
            }
        });
    }

    public void checkAssetStatus(LocalAssetsManager.AssetStatusListener assetStatusListener) {
        localAssetsManager.checkAssetStatus(localAssetPath, widevineClassicSource.getId(), assetStatusListener);
    }

    public void unregisterAsset(LocalAssetsManager.AssetRemovalListener assetRemovalListener) {
        localAssetsManager.unregisterAsset(localAssetPath, widevineClassicSource.getId(), assetRemovalListener);
    }
}
