package com.kaltura.playkit.plugin.mediaprovider.phoenix;

import android.os.Bundle;
import android.text.TextUtils;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugin.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResponseElement;
import com.kaltura.playkit.plugin.mediaprovider.base.BaseMediaProvider;
import com.kaltura.playkit.plugin.mediaprovider.phoenix.data.AssetInfo;
import com.kaltura.playkit.plugin.mediaprovider.phoenix.data.MediaFile;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider extends BaseMediaProvider {

    private PhoenixRequestsHandler requestsHandler;
    private OnCompletion completion;

    public PhoenixMediaProvider(String baseUrl){
        requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
    }

    public void load(String ks, String id, Bundle args, OnCompletion completion){
        loadAsset(ks, id, args.getString("assetReferenceType"), args.getString("format"), completion);
    }

    private void loadAsset(String ks, String id, String assetType, final String format, final OnCompletion completion){
        requestsHandler.getMediaInfo(ks, id, assetType, new OnCompletion<ResponseElement>() {
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
    public PKMediaEntry getMediaEntry() {
        return null;
    }

    @Override
    public void load(OnCompletion callback) {

    }
}
