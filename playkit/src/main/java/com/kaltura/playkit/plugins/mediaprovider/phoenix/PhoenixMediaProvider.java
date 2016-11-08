package com.kaltura.playkit.plugins.mediaprovider.phoenix;

import android.os.Bundle;
import android.text.TextUtils;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.plugins.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugins.connect.OnRequestCompletion;
import com.kaltura.playkit.plugins.connect.ResponseElement;
import com.kaltura.playkit.plugins.mediaprovider.base.OnMediaLoadCompletion;
import com.kaltura.playkit.plugins.mediaprovider.phoenix.data.AssetInfo;
import com.kaltura.playkit.plugins.mediaprovider.phoenix.data.MediaFile;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    private PhoenixRequestsHandler requestsHandler;

    public PhoenixMediaProvider(String baseUrl){
        requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
    }

    public void load(String ks, String id, Bundle args, OnRequestCompletion completion){
        loadAsset(ks, id, args.getString("assetReferenceType"), args.getString("format"), completion);
    }

    private void loadAsset(String ks, String id, String assetType, final String format, final OnRequestCompletion completion){
        requestsHandler.getMediaInfo(ks, id, assetType, new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response != null && response.isSuccess()){
                    AssetInfo asset = PhoenixParser.parseAsset(response.getResponse());
                    if(!TextUtils.isEmpty(format)){
                        for(final MediaFile file : asset.getFiles()){
                            if(file.getFormatType().equals(format)){
                                //?? only the "format" MediaFile should be parsed to MediaSource - app asked for a specific format(file)
                                ArrayList<MediaFile> newFiles = new ArrayList<>(Collections.singletonList(file));
                                asset.setFiles(newFiles);
                            }
                        }
                    }
                    PhoenixParser.getMedia(asset);
                }
            }
        });
    }




    @Override
    public void load(OnMediaLoadCompletion completion) {

    }
}
