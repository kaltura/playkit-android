package com.kaltura.playkit.plugins.mediaproviders.phoenix;

import android.text.TextUtils;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugins.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugins.connect.OnRequestCompletion;
import com.kaltura.playkit.plugins.connect.ResponseElement;
import com.kaltura.playkit.plugins.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.plugins.mediaproviders.phoenix.data.AssetInfo;
import com.kaltura.playkit.plugins.mediaproviders.phoenix.data.MediaFile;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    private PhoenixRequestsHandler requestsHandler;
    private String assetId;
    private String ks;
    private int partnerId = 0;
    private String referenceType;
    private String format;

    public PhoenixMediaProvider(String baseUrl, int partnerId, String assetId, String assetType){
        requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
    }

    public PhoenixMediaProvider(String baseUrl, String ks, String assetId, String assetType){
        requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
    }

    @Override
    public void load(OnMediaLoadCompletion completion){
        requestsHandler.getMediaInfo(ks, assetId, referenceType, new OnRequestCompletion() {
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
                    PKMediaEntry mediaEntry = PhoenixParser.getMedia(asset);
                    //TODO pass mediaEntry to completion callback
                }
            }
        });
    }


}
