package com.kaltura.playkit.plugin.mediaprovider.mock;

import android.os.Bundle;

import com.kaltura.playkit.MediaEntry;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResponseElement;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaProvider implements MediaEntryProvider {

    MockRequestHandler requestHandler = new MockRequestHandler();

    @Override
    public MediaEntry getMediaEntry() {
        return null;
    }

    @Override
    public void load(SessionKey sessionKey, String id, Bundle args, final OnCompletion callback) {
        requestHandler.getMockMedia(id, new OnCompletion<ResponseElement>() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response.isSuccess()){
                    MediaEntry mediaEntry = MockMediaParser.parseMedia(response.getResponse());
                    if(callback!=null){
                        callback.onComplete(mediaEntry);
                    }
                } else
            }
        });
    }
}
